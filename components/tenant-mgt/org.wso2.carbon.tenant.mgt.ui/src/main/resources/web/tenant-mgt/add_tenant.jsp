<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@page import="org.wso2.carbon.user.mgt.ui.UserAdminClient" %>
<%@ page import="org.wso2.carbon.stratos.common.util.CommonUtil" %>
<%@ page import="org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean" %>
<%@ page import="org.wso2.carbon.user.mgt.stub.types.carbon.UserStoreInfo" %>
<%@ page import="org.wso2.carbon.user.mgt.stub.types.carbon.UserRealmInfo" %>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.user.mgt.ui.UserAdminUIConstants" %>
<%@ page import="java.text.MessageFormat" %>
<%@ page import="java.util.ResourceBundle" %>
<%@page import="org.wso2.carbon.utils.ServerConstants" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.tenant.mgt.ui.utils.TenantMgtUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.base.ServerConfiguration" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<carbon:jsi18n
        resourceBundle="org.wso2.carbon.tenant.mgt.ui.i18n.JSResources"
        request="<%=request%>"/>
<%
    UserStoreInfo userStoreInfo = null;
    UserRealmInfo userRealmInfo = null;

    String BUNDLE = "org.wso2.carbon.userstore.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

    try {
        userRealmInfo = (UserRealmInfo) session.getAttribute(UserAdminUIConstants.USER_STORE_INFO);
        if (userRealmInfo == null) {
            String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
            String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
            ConfigurationContext configContext =
                    (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
            UserAdminClient client = new UserAdminClient(cookie, backendServerURL, configContext);
            userRealmInfo = client.getUserRealmInfo();
            session.setAttribute(UserAdminUIConstants.USER_STORE_INFO, userRealmInfo);
        }

        userStoreInfo = userRealmInfo.getPrimaryUserStoreInfo();

    } catch (Exception e) {
        String message = MessageFormat.format(resourceBundle.getString("error.while.loading.user.store.info"),
                                              e.getMessage());
%>
<script type="text/javascript">
    jQuery(document).ready(function () {
        CARBON.showErrorDialog('<%=message%>', function () {
            location.href = "add_tenant.jsp";
        });
    });
</script>
<%
    }
%>
<%

String domainName = request.getParameter("domain");
    String firstname = "";
    String lastname = "";
    String admin = "";
    String usagePlan = "";
    boolean isActive = false;
    int tenantId = -1;
    String error1 = "Tenant with the domain : " + domainName + " doesn't exist.";
    boolean isUpdating = false;
    boolean isPublicCloud = CommonUtil.isPublicCloudSetup();
    String isCloudDeployment =  ServerConfiguration.getInstance().getFirstProperty("IsCloudDeployment");
    String enableEmailUserName =  ServerConfiguration.getInstance().getFirstProperty("EnableEmailUserName");
    boolean isEmailUserNameEnabled = false;
    
    if (enableEmailUserName != null && enableEmailUserName.equalsIgnoreCase("true")){
    	isEmailUserNameEnabled = true;
    }
    
    String email = "";
    if (domainName != null && !domainName.equals("")) {
        try {
            TenantInfoBean infoBean = TenantMgtUtil.getTenant(request, config, session);
            admin = infoBean.getAdmin();
            tenantId = infoBean.getTenantId();
            email = infoBean.getEmail();
            firstname = infoBean.getFirstname();
            lastname = infoBean.getLastname();
            isActive = infoBean.getActive();
            usagePlan = infoBean.getUsagePlan();
            isUpdating = true;
            session.setAttribute("isActivatedTenant", isActive);
        } catch (Exception e) {
            e.getStackTrace();
            CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
            request.setAttribute(CarbonUIMessage.ID, uiMsg);
%>
<jsp:forward page="../admin/error.jsp"/>
<%
            return;
        }
    }

    if (domainName == null) {
        domainName = "";
    }
    if (firstname == null) {
        firstname = admin;
    }
    if (lastname == null) {
        lastname = "";
    }
%>

<fmt:bundle basename="org.wso2.carbon.tenant.mgt.ui.i18n.Resources">
<carbon:breadcrumb
        label="govern.add_tenants.menu"
        resourceBundle="org.wso2.carbon.tenant.mgt.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="js/tenant_config.js"></script>

<div id="middle">
<%if (tenantId != 0) {%>
<h2><%if (isUpdating) {%><fmt:message key="update.tenant"/>

    <%} else {%> <fmt:message
            key="register.new.organization"/><%}%></h2>

<div id="workArea">

<div id="activityReason" style="display: none;"></div>
<form id="addTenantForm" action="submit_tenant_ajaxprocessor.jsp" method="post">
    <input type="hidden" name="isUpdating" id="isUpdating" value="false">
    <input name="userNameRegex" id="userNameRegex" type="hidden" value="<%=userStoreInfo.getUserNameRegEx()%>"/>
    <input name="passwordRegex" id="passwordRegex" type="hidden value="<%=userStoreInfo.getPasswordRegEx()%>"/>
    <table class="styledLeft">
        <thead>
        <tr>
            <th>
                <fmt:message key="domain.information"/>
            </th>
        </tr>
        </thead>
        <tbody>
        <tr>
            <td class="nopadding">
                <table class="normal-nopadding" cellspacing="0">
                    <tbody>
                    <tr>
                        <td><fmt:message key="domain"/>
                            <%if (!isUpdating) { %> <span class="required">*</span> <% }%>
                        </td>
                        <td colspan="2"><input
                                onchange="fillAdminValue();" <%if (isUpdating) { %>
                                readonly="true" <% }%> type="text" name="domain"
                                id="domain" style="width:400px"
                                value="<%=domainName%>"/>
                        </td>
                    </tr>
                    <%if (!isUpdating) { %>
                    <tr>
                        <td></td>
                        <td colspan="2">Use a domain for your organization,in the format
                            "example.com",This domain should be unique.
                        </td>
                    </tr>
                    <% }%>

                    <%if (isUpdating) { %>
                    <tr>
                        <td><fmt:message key="tenant.id"/>
                        </td>
                        <td colspan="2"><input
                                onchange="fillAdminValue();"
                                readonly="true" type="text" name="tenantId"
                                id="tenantId" style="width:400px"
                                value="<%=tenantId%>"/>
                        </td>
                    </tr>
                    <% }

                    %>

                    <tr>
                        <td colspan="3" class="middle-header"><fmt:message
                                key="usage.plan.information"/></td>

                    </tr>
                    <tr>
                        <td>
                            <fmt:message key="select.usage.plan.for.tenant"/><span
                                class="required">*</span>
                        </td>
                        <td>
                            <select name="usage-plan-name" id="usage-plan-name">
                            </select>
                            <%
                                if (!CommonUtil.getStratosConfig().getUsagePlanURL().equals("")) {
                            %>
                            <a href=<%=CommonUtil.getStratosConfig().getUsagePlanURL()%>
                                       target=<%=CommonUtil.getStratosConfig().getUsagePlanURL()%>>
                                <b>More info</b></a>
                            <% } %>
                        </td>
                        <td>
                            <% if (usagePlan.length() > 2) {
                            %>
                            Your Current Usage Plan is : <%=usagePlan%>
                            <%}%>
                        </td>
                    <tr>
                        <td></td>
                        <td colspan="2"><fmt:message key="select.package.message"/>
                        </td>
                    </tr>


                    <tr>
                        <td colspan="3" class="middle-header"><fmt:message
                                key="tenant.admin"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="admin.firstname"/><span
                                class="required">*</span></td>
                        <td colspan="2"><input type="text" name="admin-firstname"
                                               id="admin-firstname" style="width:400px"
                                               value="<%=firstname%>"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="admin.lastname"/><span
                                class="required">*</span></td>
                        <td colspan="2"><input type="text" name="admin-lastname"
                                               id="admin-lastname" style="width:400px"
                                               value="<%=lastname%>"/></td>
                    </tr>


                    <tr>
                        <td><fmt:message key="admin.username"/>
                            <%if (!isUpdating) { %>
                            <span class="required">*</span></td>
                        <%}%>
                        <td colspan="2"><input <%if (isUpdating) {%>
                                readonly="true" <%}%> type="text" name="admin"
                                id="admin" style="width:400px" value="<%=admin%>"
                                onchange="isDomainNameAvailable();"/><span
                                id="adminValue"></span></td>
                    </tr>

                    <tr>
                        <td><%if (isUpdating) {%><fmt:message
                                key="new.admin.password"/><%} else {%><fmt:message
                                key="admin.password"/><%}%>
                            <%if (!isUpdating) {%><span class="required">*</span></td>
                        <%}%>
                        <td colspan="2"><input type="password" name="admin-password"
                                               id="admin-password" style="width:400px"/>
                        </td>
                    </tr>
                    <tr>
                        <td><%if (isUpdating) {%><fmt:message
                                key="new.admin.password.repeat"/><%} else {%><fmt:message
                                key="admin.password.repeat"/><%}%>
                            <%if (!isUpdating) {%><span class="required">*</span></td>
                        <%}%>
                        <td colspan="2"><input type="password"
                                               name="admin-password-repeat"
                                               id="admin-password-repeat"
                                               style="width:400px"/></td>
                    </tr>
                    <% if (!isEmailUserNameEnabled)  { %>
                    <tr>
                        <td colspan="3" class="middle-header"><fmt:message
                                key="contact.details"/></td>
                    </tr>
                    <tr>
                        <td><fmt:message key="admin.email"/><span
                                class="required">*</span></td>
                        <td colspan="2"><input type="text" name="admin-email"
                                               id="admin-email" style="width:400px"
                                               value="<%=email%>"/></td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </td>
        </tr>
        <tr id="buttonRow">
            <td class="buttonRow">
                <input class="button" type="button"
                        <% if (isUpdating) { %> value="Update" <% } else { %>
                       value="Save" <% }%>
                       onclick="addTenant(<%=isUpdating?"true":"false"%>, <%=isPublicCloud?"true":"false"%>,<%=isEmailUserNameEnabled?"true":"false"%>)"/>
            </td>
        </tr>
        <tr id="waitMessage" style="display:none">
            <td>
                <div style="font-size:13px !important;margin-top:10px;margin-bottom:10px;">
                    <img
                            src="images/ajax-loader.gif" align="left" hspace="20"/>Please
                    wait until the Service is
                    importing to the Registry...
                </div>
            </td>
        </tr>
        </tbody>
    </table>
    <%
        // the tenantId field appears only for an update of existing tenant
        if (isUpdating) {
    %>
    <input name="tenantId" type="hidden" value="<%=tenantId%>"/>
    <%
        }
    %>
    <%} else {%>
    <tr>
        <th>
            <%=error1 %>
        </th>
    </tr>
    <%}%>
</form>

<form id="activateTenantForm" action="activate_tenant_ajaxprocessor.jsp" method="post">
    <%if ((isUpdating) && (tenantId > 0)) {%>
    <table class="styledLeft">
        <thead>
        <tr>
            <th>
                <fmt:message key="activate.deactivate"/>
            </th>
        </tr>
        </thead>
        <tbody>

            <% if (isActive) { %>
        <tr>
            <td colspan="3"><fmt:message key="deactivate.account.msg"/></td>
        </tr>
            <% } else {%>
        <tr>
            <td colspan="3"><fmt:message key="activate.account.msg"/></td>
        </tr>
            <% }%>

        <tr id="buttonRow2">
            <td class="buttonRow">
                <input class="button" type="button" name="activateButton" id="activateButton"
                       onclick="return activateDeactivate('<%=domainName%>','<%=isActive%>');"
                        <% if (isActive) { %> value="<fmt:message key="deactivate.account.btn"/>"
                <% } else { %> value="<fmt:message key="activate.account.btn"/>" <% } %>/>
            </td>

            <input type="hidden" name="activatingDomain" id="activatingDomain" value="<%=domainName%>"/>

        </tr>
        </tbody>
    </table>
    <%
        }
    %>
</form>

<br/>
<script type="text/javascript">refreshFillAdminValue()</script>

</div>
</div>
</fmt:bundle>
<script type="text/javascript">
    var packageInfo;
    jQuery(document).ready(
                          function() {
                              jQuery.ajax({
                                  type: 'POST',
                                  url: 'get_package_info_ajaxprocessor.jsp',
                                  dataType: 'json',
                                  data: 'plan=0',
                                  async: false,
                                  success: function(data) {
                                      packageInfo = data;
                                  },
                                  error:function (xhr, ajaxOptions, thrownError) {
                                      CARBON.showErrorDialog('Could not get package information.');
                                  }
                              });

                              var charge;
                              var name;
                              var isCloud = <%= isCloudDeployment %>;

                              if (!isCloud) {
                                  String
                                  demoOption = "Demo"
                                  option = document.createElement("option");
                                  option.value = demoOption;
                                  option.selected = demoOption;
                                  option.innerHTML = demoOption;
                                  document.getElementById('usage-plan-name').appendChild(option);

                              } else {
                                  for (var i = 0; i < packageInfo.length; i++) {
                                      charge = packageInfo[i].subscriptionCharge;
                                      name = packageInfo[i].name;
                                      if (name == '<%=usagePlan%>') {
                                          option = document.createElement("option");
                                          option.value = name;
                                          option.selected = name;
                                          option.innerHTML = name;
                                          document.getElementById('usage-plan-name').appendChild(option);

                                      }
                                      else {
                                          option = document.createElement("option");
                                          option.value = name;
                                          option.innerHTML = name
                                          document.getElementById('usage-plan-name').appendChild(option);
                                      }
                                  }
                              }
                          }
            );
</script>
