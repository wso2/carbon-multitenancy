/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.redirector.servlet.ui.filters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.redirector.servlet.ui.clients.RedirectorServletServiceClient;
import org.wso2.carbon.ui.CarbonUIUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class AllPagesFilter implements Filter {
    private static final Log log = LogFactory.getLog(AllPagesFilter.class);
    private static Map<String, Boolean> tenantExistMap = new HashMap<>();

    ServletContext context;

    public void init(FilterConfig filterConfig) throws ServletException {
        context = filterConfig.getServletContext();
    }

    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse, FilterChain filterChain) throws
            IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest)) {
            // Not an HTTP request, skip filtering
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String requestedURI = request.getRequestURI();

        String contextPath = request.getContextPath();
        if (contextPath == null || ("/").equals(contextPath)) {
        	contextPath = "";
        }
        
        StringTokenizer tokenizer = new StringTokenizer(requestedURI.substring(contextPath.length() + 1), "/");
        String[] firstUriTokens = new String[2];
        int i = 0;
        while (tokenizer.hasMoreElements()) {
            firstUriTokens[i] = tokenizer.nextToken();
            i++;
            if (i > 1) {
                break;
            }
        }
        if (i > 1 && firstUriTokens[0].equals("t")) {
            if (requestedURI.startsWith("//")) {
                requestedURI = requestedURI.replaceFirst("//", "/");
            }
            String path = requestedURI.substring(contextPath.length() + firstUriTokens[0].length() +
                    firstUriTokens[1].length() + 2);

            // need to validate the tenant exists
            String tenantDomain = firstUriTokens[1];
            boolean tenantExists = true;
            boolean tenantActive = true;

            if (tenantExistMap.get(tenantDomain) == null) {
                // we have to call the service :(
                RedirectorServletServiceClient client;
                try {
                    client = new RedirectorServletServiceClient(context, request.getSession());
                } catch (Exception e) {
                    String msg = "Error in constructing RedirectorServletServiceClient.";
                    log.error(msg, e);
                    throw new ServletException(msg, e);
                }

                try {
                    String status = client.validateTenant(tenantDomain);
                    tenantExists = !StratosConstants.INVALID_TENANT.equals(status);
                    if (tenantExists && StratosConstants.ACTIVE_TENANT.equals(status)) {
                        //tenantExists = true;
                        tenantActive = true;
                    }
                } catch (Exception e) {
                    String msg = "Error in checking the existing of the tenant domain: " +
                            tenantDomain + ".";
                    log.error(msg, e);
                    throw new ServletException(msg, e);
                }
            }
            // we have some backup stuff, if the tenant doesn't exists
            if (tenantExists) {
                if (tenantActive) {
                    // we put this to hash only if the original tenant domain exist
                    tenantExistMap.put(tenantDomain, true);
                } else {
                    String errorPage = contextPath +
                            "/carbon/admin/error.jsp?The Requested tenant domain: " +
                            tenantDomain + " is inactive.";
                    RequestDispatcher requestDispatcher =
                            request.getRequestDispatcher(errorPage);
                    requestDispatcher.forward(request, servletResponse);
                    return;
                }
            } else {
                String errorPage = contextPath +
                        "/carbon/admin/error.jsp?The Requested tenant domain: " +
                        tenantDomain + " doesn't exist.";
                RequestDispatcher requestDispatcher =
                        request.getRequestDispatcher(errorPage);
                requestDispatcher.forward(request, servletResponse);
                return;
            }
            request.setAttribute(MultitenantConstants.TENANT_DOMAIN, tenantDomain);

            if (path.indexOf("/admin/index.jsp") >= 0) {
                // we are going to apply the login.jsp filter + tenant specific filter both in here
                path = path.replaceAll("/admin/index.jsp", "/tenant-dashboard/index.jsp");
                request.setAttribute(StratosConstants.TENANT_SPECIFIC_URL_RESOLVED, "1");
            }
            if (path.indexOf("admin/docs/userguide.html") >= 0) {
                // we are going to apply the dasbhoard docs.jsp filter +
                // tenant specif filter both in here
                path = path.replaceAll("admin/docs/userguide.html",
                        "tenant-dashboard/docs/userguide.html");
                request.setAttribute(StratosConstants.TENANT_SPECIFIC_URL_RESOLVED, "1");
            }
            if ("".equals(path) || "/".equals(path) || "/carbon".equals(path) ||
                    "/carbon/".equals(path) || "/carbon/admin".equals(path) ||
                    "/carbon/admin/".equals(path)) {
                // we have to redirect the root to the login page directly
                path = CarbonUIUtil.resolveAdminConsoleBaseURL(contextPath, "/carbon/admin/login.jsp", request);
                if (log.isDebugEnabled()) {
                    log.debug("Resolved the admin console base path to : " + path);
                }
            	((HttpServletResponse) servletResponse).sendRedirect(path);
                return;
            }
            RequestDispatcher requestDispatcher = null;

            // Strategy 1: Use request.getRequestDispatcher() - this works best when both
            // the filter and target servlet are in the same web application (even if filter
            // is registered via OSGi Whiteboard and servlet via web.xml)
            requestDispatcher = request.getRequestDispatcher(path);

            // Strategy 2: Try ServletContext.getRequestDispatcher() as fallback
            if (requestDispatcher == null) {
                requestDispatcher = context.getRequestDispatcher(path);
            }

            // Strategy 3: Try cross-context dispatch if enabled
            if (requestDispatcher == null) {
                // Try to get the root context where AxisServlet might be registered
                ServletContext rootContext = context.getContext("/");
                if (rootContext != null && rootContext != context) {
                    requestDispatcher = rootContext.getRequestDispatcher(path);
                    if (requestDispatcher != null) {
                        log.debug("Using cross-context RequestDispatcher from root context for path: " + path);
                    }
                }
            }

            if (requestDispatcher != null) {
                requestDispatcher.forward(request, servletResponse);
            } else {
                log.error("RequestDispatcher is null for path: '" + path +
                        "'. This filter is registered via OSGi HTTP Whiteboard but the target " +
                        "AxisServlet is registered via web.xml in a different servlet context. " +
                        "Consider registering this filter in web.xml instead.");

                ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Unable to forward tenant request to: " + path +
                                ". Cross-context dispatch not available.");
            }
        } else {
            // Not a tenant URL, continue with normal processing
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("AllPagesFilter destroyed");
        }
    }
}
