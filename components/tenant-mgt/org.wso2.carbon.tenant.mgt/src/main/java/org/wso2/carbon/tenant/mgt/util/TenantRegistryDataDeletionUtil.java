/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

public class TenantRegistryDataDeletionUtil {

    public static final Log log = LogFactory.getLog(TenantRegistryDataDeletionUtil.class);

    private static final String DELETE_CLUSTER_LOCK = "DELETE FROM REG_CLUSTER_LOCK WHERE REG_TENANT_ID = ?";
    private static final String DELETE_LOG = "DELETE FROM REG_LOG WHERE REG_TENANT_ID = ?";
    private static final String DELETE_ASSOCIATION = "DELETE FROM REG_ASSOCIATION WHERE REG_TENANT_ID = ?";
    private static final String DELETE_SNAPSHOT = "DELETE FROM REG_SNAPSHOT WHERE REG_TENANT_ID = ?";
    private static final String DELETE_RESOURCE_COMMENT = "DELETE FROM REG_RESOURCE_COMMENT WHERE REG_TENANT_ID = ?";
    private static final String DELETE_COMMENT = "DELETE FROM REG_COMMENT WHERE REG_TENANT_ID = ?";
    private static final String DELETE_RESOURCE_RATING = "DELETE FROM REG_RESOURCE_RATING WHERE REG_TENANT_ID = ?";
    private static final String DELETE_RATING = "DELETE FROM REG_RATING WHERE REG_TENANT_ID = ?";
    private static final String DELETE_RESOURCE_TAG = "DELETE FROM REG_RESOURCE_TAG WHERE REG_TENANT_ID = ?";
    private static final String DELETE_TAG = "DELETE FROM REG_TAG WHERE REG_TENANT_ID = ?";
    private static final String DELETE_RESOURCE_PROPERTY = "DELETE FROM REG_RESOURCE_PROPERTY WHERE REG_TENANT_ID = ?";
    private static final String DELETE_PROPERTY = "DELETE FROM REG_PROPERTY WHERE REG_TENANT_ID = ?";
    private static final String DELETE_RESOURCE_HISTORY = "DELETE FROM REG_RESOURCE_HISTORY WHERE REG_TENANT_ID = ?";
    private static final String DELETE_CONTENT_HISTORY = "DELETE FROM REG_CONTENT_HISTORY WHERE REG_TENANT_ID = ?";
    private static final String DELETE_RESOURCE = "DELETE FROM REG_RESOURCE WHERE REG_TENANT_ID = ?";
    private static final String DELETE_CONTENT = "DELETE FROM REG_CONTENT WHERE REG_TENANT_ID = ?";
    private static final String DELETE_PATH = "DELETE FROM REG_PATH WHERE REG_TENANT_ID = ?";
    private static final String DELETE_ADMIN_REGISTRY_RESORUCE = "DELETE FROM REG_RESOURCE WHERE REG_PATH_ID IN " +
            "(SELECT DISTINCT REG_PATH_ID FROM REG_PATH WHERE REG_PATH_VALUE LIKE ?)";
    private static final String DELETE_ADMIN_REGISTRY_PATH = "DELETE FROM REG_PATH WHERE REG_PATH_VALUE LIKE ?";

    /**
     * Delete all tenant information related to tenant stored in REG tables.
     *
     * @param tenantId id of tenant whose data should be deleted
     * @param conn     database connection object
     * @throws SQLException thrown if an error occurs while executing the queries
     */
    protected static void deleteTenantRegistryData(int tenantId, Connection conn) throws SQLException {

        try {
            conn.setAutoCommit(false);
            executeDeleteQuery(conn, DELETE_CLUSTER_LOCK, tenantId);
            executeDeleteQuery(conn, DELETE_LOG, tenantId);
            executeDeleteQuery(conn, DELETE_ASSOCIATION, tenantId);
            executeDeleteQuery(conn, DELETE_SNAPSHOT, tenantId);
            executeDeleteQuery(conn, DELETE_RESOURCE_COMMENT, tenantId);
            executeDeleteQuery(conn, DELETE_COMMENT, tenantId);
            executeDeleteQuery(conn, DELETE_RESOURCE_RATING, tenantId);
            executeDeleteQuery(conn, DELETE_RATING, tenantId);
            executeDeleteQuery(conn, DELETE_RESOURCE_TAG, tenantId);
            executeDeleteQuery(conn, DELETE_TAG, tenantId);
            executeDeleteQuery(conn, DELETE_RESOURCE_PROPERTY, tenantId);
            executeDeleteQuery(conn, DELETE_PROPERTY, tenantId);
            executeDeleteQuery(conn, DELETE_RESOURCE_HISTORY, tenantId);
            executeDeleteQuery(conn, DELETE_CONTENT_HISTORY, tenantId);
            executeDeleteQuery(conn, DELETE_RESOURCE, tenantId);
            executeDeleteQuery(conn, DELETE_CONTENT, tenantId);
            executeDeleteQuery(conn, DELETE_PATH, tenantId);
            executeDeleteQueryWithLikeOperator(conn, DELETE_ADMIN_REGISTRY_RESORUCE, tenantId);
            executeDeleteQueryWithLikeOperator(conn, DELETE_ADMIN_REGISTRY_PATH, tenantId);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            String errorMsg = "An error occurred while deleting registry data for tenant: " + tenantId;
            log.error(errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            conn.close();
        }
    }

    private static void executeDeleteQuery(Connection conn, String query, int tenantId)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            String errMsg = "Error executing query " + query + " for tenant: " + tenantId;
            log.error(errMsg, e);
            throw new SQLException(errMsg, e);
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
    private static void executeDeleteQueryWithLikeOperator(Connection conn, String query, int tenantId)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            String param = "%/" + String.valueOf(tenantId) + "/%";
            ps.setNString(1, param);
            ps.executeUpdate();
        } catch (SQLException e) {
            String errMsg = "Error executing query " + query + " for tenant: " + tenantId;
            log.error(errMsg, e);
            throw new SQLException(errMsg, e);
        }
    }
}
