/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.tenant.mgt.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TenantIDNDataDeletionUtil {

    public static final Log log = LogFactory.getLog(TenantIDNDataDeletionUtil.class);

    private static final String DELETE_CM_PURPOSE_CATEGORY = "DELETE FROM CM_PURPOSE_CATEGORY WHERE TENANT_ID = ?";
    private static final String DELETE_IDN_CLAIM = "DELETE FROM IDN_CLAIM WHERE TENANT_ID = ?";
    private static final String DELETE_IDN_CLAIM_DIALECT = "DELETE FROM IDN_CLAIM_DIALECT WHERE TENANT_ID = ?";
    private static final String DELETE_IDN_CLAIM_MAPPED_ATTRIBUTE = "DELETE FROM IDN_CLAIM_MAPPED_ATTRIBUTE WHERE TENANT_ID = ?";
    private static final String DELETE_IDN_CLAIM_MAPPING = "DELETE FROM IDN_CLAIM_MAPPING WHERE TENANT_ID = ?";
    private static final String DELETE_IDN_CLAIM_PROPERTY = "DELETE FROM IDN_CLAIM_PROPERTY WHERE TENANT_ID = ?";
    private static final String DELETE_IDN_OIDC_SCOPE = "DELETE FROM IDN_OIDC_SCOPE WHERE TENANT_ID = ?";
    private static final String DELETE_IDP = "DELETE FROM IDP WHERE TENANT_ID = ?";
    private static final String DELETE_IDP_AUTHENTICATOR = "DELETE FROM IDP_AUTHENTICATOR WHERE TENANT_ID = ?";
    private static final String DELETE_IDP_AUTHENTICATOR_PROPERTY = "DELETE FROM IDP_AUTHENTICATOR_PROPERTY WHERE TENANT_ID = ?";
    private static final String DELETE_IDP_METADATA = "DELETE FROM IDP_METADATA WHERE TENANT_ID = ?";
    private static final String DELETE_IDP_PROVISIONING_CONFIG = "DELETE FROM IDP_PROVISIONING_CONFIG WHERE TENANT_ID = ?";
    private static final String DELETE_IDP_PROV_CONFIG_PROPERTY = "DELETE FROM IDP_PROV_CONFIG_PROPERTY WHERE TENANT_ID = ?";

    /**
     * Delete all tenant information related to tenant stored in IDN tables
     *
     * @param tenantId id of tenant whose data should be deleted
     * @param conn     database connection object
     * @throws SQLException thrown if an error occurs while executing the queries
     */
    protected static void deleteTenantIDNData(int tenantId, Connection conn) throws SQLException {
        try {
            conn.setAutoCommit(false);
            executeDeleteQuery(conn, DELETE_CM_PURPOSE_CATEGORY, tenantId);
            executeDeleteQuery(conn, DELETE_IDN_CLAIM, tenantId);
            executeDeleteQuery(conn, DELETE_IDN_CLAIM_DIALECT, tenantId);
            executeDeleteQuery(conn, DELETE_IDN_CLAIM_MAPPED_ATTRIBUTE, tenantId);
            executeDeleteQuery(conn, DELETE_IDN_CLAIM_MAPPING, tenantId);
            executeDeleteQuery(conn, DELETE_IDN_CLAIM_PROPERTY, tenantId);
            executeDeleteQuery(conn, DELETE_IDN_OIDC_SCOPE, tenantId);
            executeDeleteQuery(conn, DELETE_IDP, tenantId);
            executeDeleteQuery(conn, DELETE_IDP_AUTHENTICATOR, tenantId);
            executeDeleteQuery(conn, DELETE_IDP_AUTHENTICATOR_PROPERTY, tenantId);
            executeDeleteQuery(conn, DELETE_IDP_METADATA, tenantId);
            executeDeleteQuery(conn, DELETE_IDP_PROVISIONING_CONFIG, tenantId);
            executeDeleteQuery(conn, DELETE_IDP_PROV_CONFIG_PROPERTY, tenantId);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            String errorMsg = "An error occurred while deleting identity data for tenant: " + tenantId;
            throw new SQLException(errorMsg, e);
        } finally {
            conn.close();
        }
    }
    
    /**
     * Initialise prepared statements for given query and execute the prepared statement.
     *
     * @param conn     database connection object
     * @param query    query for prepared statement
     * @param tenantId tenant id
     * @throws SQLException thrown if an error occurs while executing the query.
     */
    private static void executeDeleteQuery(Connection conn, String query, int tenantId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            String errMsg = "Error executing query " + query + " for tenant: " + tenantId;
            throw new SQLException(errMsg, e);
        }
    }
}
