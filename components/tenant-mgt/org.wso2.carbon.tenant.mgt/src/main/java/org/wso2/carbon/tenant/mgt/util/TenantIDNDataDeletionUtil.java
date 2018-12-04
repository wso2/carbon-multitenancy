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

    /**
     * Delete all tenant information related to tenant stored in IDN tables
     *
     * @param tenantId id of tenant whose data should be deleted
     * @param conn     database connection object
     * @throws SQLException thrown if an error occurs while executing the queries
     */
    protected static void deleteTenantIDNData(int tenantId, Connection conn) throws Exception {

        try {
            conn.setAutoCommit(false);
            String deleteCM_PURPOSE_CATEGORYSql = "DELETE FROM CM_PURPOSE_CATEGORY WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteCM_PURPOSE_CATEGORYSql, tenantId);

            String deleteIDN_CLAIMSql = "DELETE FROM IDN_CLAIM WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDN_CLAIMSql, tenantId);

            String deleteIDN_CLAIM_DIALECTSql = "DELETE FROM IDN_CLAIM_DIALECT WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDN_CLAIM_DIALECTSql, tenantId);

            String deleteIDN_CLAIM_MAPPED_ATTRIBUTESql = "DELETE FROM IDN_CLAIM_MAPPED_ATTRIBUTE WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDN_CLAIM_MAPPED_ATTRIBUTESql, tenantId);

            String deleteIDN_CLAIM_MAPPINGSql = "DELETE FROM IDN_CLAIM_MAPPING WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDN_CLAIM_MAPPINGSql, tenantId);

            String deleteIDN_CLAIM_PROPERTYSql = "DELETE FROM IDN_CLAIM_PROPERTY WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDN_CLAIM_PROPERTYSql, tenantId);

            String deleteIDN_OIDC_SCOPESql = "DELETE FROM IDN_OIDC_SCOPE WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDN_OIDC_SCOPESql, tenantId);

            String deleteIDPSql = "DELETE FROM IDP WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDPSql, tenantId);

            String deleteIDP_AUTHENTICATORSql = "DELETE FROM IDP_AUTHENTICATOR WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDP_AUTHENTICATORSql, tenantId);

            String deleteIDP_AUTHENTICATOR_PROPERTYSql = "DELETE FROM IDP_AUTHENTICATOR_PROPERTY WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDP_AUTHENTICATOR_PROPERTYSql, tenantId);

            String deleteIDP_METADATASql = "DELETE FROM IDP_METADATA WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDP_METADATASql, tenantId);

            String deleteIDP_PROVISIONING_CONFIGSql = "DELETE FROM IDP_PROVISIONING_CONFIG WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDP_PROVISIONING_CONFIGSql, tenantId);

            String deleteIDP_PROV_CONFIG_PROPERTYSql = "DELETE FROM IDP_PROV_CONFIG_PROPERTY WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteIDP_PROV_CONFIG_PROPERTYSql, tenantId);

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            String errorMsg = "An error occurred while deleting registry data for tenant: " + tenantId;
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
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
     * @throws Exception thrown if an error occurs while executing the query.
     */
    private static void executeDeleteQuery(Connection conn, String query, int tenantId)
            throws Exception {

        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(query);
            ps.setInt(1, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            String errMsg = "Error executing query " + query + " for tenant: " + tenantId;
            log.error(errMsg, e);
            throw new Exception(errMsg, e);
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }
}
