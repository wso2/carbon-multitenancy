/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.tenant.mgt.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * This class is responsible for deleting product specific tenant data
 */
public class TenantDataDeletionUtil {

    public static final Log log = LogFactory.getLog(TenantDataDeletionUtil.class);

    public static void deleteProductSpecificTenantData(Connection conn, String tableName, int tenantId)
    {
        try {
            conn.setAutoCommit(false);
            String deleteUserPermissionSql = "DELETE FROM "+tableName+" WHERE TENANT_ID = ?";
            executeDeleteQuery(conn, deleteUserPermissionSql, tenantId);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

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
