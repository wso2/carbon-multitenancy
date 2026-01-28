/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.redirector.servlet.ui.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.wso2.carbon.redirector.servlet.ui.filters.AllPagesFilter;
import org.wso2.carbon.redirector.servlet.ui.servlets.TenantRedirectorServlet;
import org.wso2.carbon.utils.CarbonUtils;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

@Component(immediate = true)
public class TenantServletRegistrar {

    private ServiceRegistration<Servlet> servletReg;
    private ServiceRegistration<Filter> filterReg;

    @Activate
    protected void activate(BundleContext ctx) {

        String carbonContextName = "carbonContext";
        String carbonServletPattern = "/t/*";
        String carbonContextFilter = "(&(objectClass=" + ServletContextHelper.class.getName() + ")" +
                "(osgi.http.whiteboard.context.name=" + carbonContextName + "))";
        registerServletContextHelper(ctx, carbonContextFilter, carbonContextName, carbonServletPattern, 100);
    }

    private void registerServletContextHelper(BundleContext ctx, String filter, String contextName,
                                              String servletPattern, int serviceRanking) {
        try {
            // Now register servlet and filter
            ServiceTracker<ServletContextHelper, ServletContextHelper> tracker = new ServiceTracker<>(ctx,
                    ctx.createFilter(filter),
                    new ServiceTrackerCustomizer<ServletContextHelper, ServletContextHelper>() {
                        @Override
                        public ServletContextHelper addingService(ServiceReference<ServletContextHelper> ref) {
                            // Now register servlet and filter
                            registerServletAndFilter(ctx, contextName, servletPattern, serviceRanking);
                            return ctx.getService(ref);
                        }

                        @Override
                        public void modifiedService(ServiceReference<ServletContextHelper> serviceReference,
                                                    ServletContextHelper servletContextHelper) {

                        }

                        @Override
                        public void removedService(ServiceReference<ServletContextHelper> serviceReference,
                                                   ServletContextHelper servletContextHelper) {
                            filterReg.unregister();
                            servletReg.unregister();
                        }
                    });
            tracker.open();
        } catch (InvalidSyntaxException e) {
            // handle
        }
    }

    private void registerServletAndFilter(BundleContext ctx, String contextName, String servletPattern, int serviceRanking) {
        try {
            if (!CarbonUtils.isRemoteRegistry()) {
                // Register servlet
                Dictionary<String, Object> servletProps = new Hashtable<>();
                servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, servletPattern);
                servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "TenantRedirectorServlet");
                servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                        "(osgi.http.whiteboard.context.name=" + contextName + ")");
                servletProps.put(Constants.SERVICE_RANKING, serviceRanking);
                servletReg = ctx.registerService(Servlet.class, new TenantRedirectorServlet(), servletProps);

                // Register filter
                Dictionary<String, Object> filterProps = new Hashtable<>();
                filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET, "TenantRedirectorServlet");
                filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "AllPagesFilter");
                filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                        "(osgi.http.whiteboard.context.name=" + contextName + ")");
                filterProps.put(Constants.SERVICE_RANKING, serviceRanking);
                filterReg = ctx.registerService(Filter.class, new AllPagesFilter(), filterProps);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
