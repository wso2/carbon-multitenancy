<!--
~ Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@ page import="org.wso2.carbon.tenant.mgt.ui.utils.TenantMgtUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<script type="text/javascript" src="../admin/js/jquery.js"></script>
<script type="text/javascript" src="../admin/js/jquery.form.js"></script>
<script type="text/javascript" src="../dialog/js/jqueryui/jquery-ui.min.js"></script>
<script type="text/javascript" src="js/tenant_config.js"></script>

<%--<carbon:jsi18n--%>
<%--resourceBundle="org.wso2.carbon.tenant.mgt.ui.i18n.JSResources"--%>
<%--request="<%=request%>" />--%>

<div id="middle">
    <%
        try {
            TenantMgtUtil.deleteTenant(request, config, session);
    %>
    <script type="text/javascript">
        forwardAfterDeletion();
    </script>
    <%

    } catch (Exception e) {
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        request.setAttribute(CarbonUIMessage.ID, uiMsg);
    %>
    <jsp:forward page="../admin/error.jsp"/>
    <%
            return;
        }
    %>
</div>