package com.example.snmp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.transform.Result;

import java.sql.CallableStatement;

import com.example.snmp.App;
import com.example.snmp.DataSourceSingleton;
import com.example.snmp.NodeSerialOid;
import com.example.snmp.Node;

//for logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dao {
    private final DataSource dataSource;
    // private static TransportMapping<UdpAddress> transport;
    private static final Logger logger = LoggerFactory.getLogger(Dao.class);

    public Dao(String mode) {
        this.dataSource = DataSourceSingleton.getDataSource(mode);
    }

    public Connection getDbConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /*
     *****************************************************************************************************************
     * NODE TYPE SECTION * NODE TYPE SECTION * NODE TYPE SECTION * NODE TYPE SECTION
     * * NODE TYPE SECTION * NODE TYPE SECTION
     ******************************************************************************************************************
     */

    public List<NodeType> getNodeTypes(String service) {
        List<NodeType> nodeTypes = new ArrayList<>();
        String query = "SELECT * FROM nodetype WHERE service = ?";
        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, service);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    NodeType nodeType = new NodeType();
                    nodeType.setId(rs.getInt("id"));
                    nodeType.setName(rs.getString("name"));
                    nodeTypes.add(nodeType);
                }
            }
        } catch (SQLException e) {
            // System.err.println("Database error in getNodeTypes: " + e.getMessage());
            logger.error("Database error in getNodeTypes: " + e.getMessage());
        }
        return nodeTypes;
    }

    public NodeType getNodeTypeById(int id) {
        NodeType nodeType = null;
        String query = "SELECT name, vendor, onuserial, onudesc, lastondate,onudistance,onurxpower FROM nodetype WHERE id = ?";

        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    nodeType = new NodeType();
                    nodeType.setId(id);
                    nodeType.setName(rs.getString("name"));
                    nodeType.setVendor(rs.getString("vendor"));
                    nodeType.setOidOnuSerial(rs.getString("onuserial"));
                    nodeType.setOidOnuDescription(rs.getString("onudesc"));
                    nodeType.setOidOnuDistance(rs.getString("onudistance"));
                    nodeType.setOidOnuRxPower(rs.getString("onurxpower"));

                    String lastOnDateStr = rs.getString("lastondate");
                    if (lastOnDateStr != null && !lastOnDateStr.isEmpty()) {
                        nodeType.setOidOnuLastOnDateTime(lastOnDateStr);
                    } else {
                        nodeType.setOidOnuLastOnDateTime("0000-00-00 00:00:00");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error in getNodeTypeById: " + e.getMessage());
        }

        return nodeType;
    }

    public List<Node> getAllNodes() {
        List<Node> nodes = new ArrayList<>();
        String query = "SELECT * FROM node WHERE service like ? ";
        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY)) {

            stmt.setString(1, "ftth");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Node node = new Node();
                    node.setId(rs.getInt("id"));
                    node.setName(rs.getString("name"));
                    node.setType(rs.getInt("type"));
                    node.setIp(rs.getString("ip"));
                    node.setAssignedID(rs.getString("assignedID"));
                    node.setShelfncard(rs.getString("shelfncard"));
                    node.setService(rs.getString("service"));
                    node.setPortspercard(rs.getInt("portspercard"));
                    node.setRegion(rs.getString("region"));
                    node.setExchange(rs.getString("exchange"));
                    node.setSnmpcommunity(rs.getString("snmpcommunity"));
                    node.setSnmpwritecommunity(rs.getString("snmpwritecommunity"));
                    node.setSysname(rs.getString("sysname"));
                    node.setTimestamp(rs.getTimestamp("timestamp"));

                    nodes.add(node);
                }
            }
        } catch (SQLException e) {
            // System.err.println("Database error in getNodes: " + e.getMessage());
            logger.error("Database error in getNodes: " + e.getMessage());
        }
        return nodes;
    }

    /*
     *****************************************************************************************************************
     * NODE SECTION * NODE SECTION * NODE SECTION * NODE SECTION * NODE SECTION
     ******************************************************************************************************************
     */
    public List<Node> getNodes(int nodeType, String region) {
        List<Node> nodes = new ArrayList<>();
        String query = "SELECT * FROM node WHERE type = ?";
        Boolean isRegionSelected = !region.equalsIgnoreCase("all");

        if (isRegionSelected) {
            query = "SELECT * FROM node WHERE type = ? and region =?";

        }

        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, nodeType);
            if (isRegionSelected) {
                stmt.setString(2, region);

            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Node node = new Node();
                    node.setId(rs.getInt("id"));
                    node.setName(rs.getString("name"));
                    node.setType(rs.getInt("type"));
                    node.setIp(rs.getString("ip"));
                    node.setAssignedID(rs.getString("assignedID"));
                    node.setShelfncard(rs.getString("shelfncard"));
                    node.setService(rs.getString("service"));
                    node.setPortspercard(rs.getInt("portspercard"));
                    node.setRegion(rs.getString("region"));
                    node.setExchange(rs.getString("exchange"));
                    node.setSnmpcommunity(rs.getString("snmpcommunity"));
                    node.setSnmpwritecommunity(rs.getString("snmpwritecommunity"));
                    node.setSysname(rs.getString("sysname"));
                    node.setTimestamp(rs.getTimestamp("timestamp"));

                    nodes.add(node);
                }
            }
        } catch (SQLException e) {
            // System.err.println("Database error in getNodes: " + e.getMessage());
            logger.error("Database error in getNodes: " + e.getMessage());
        }
        return nodes;
    }

    public List<NodeSerialOid> getNodeSerialsOid(int nodeId) {
        List<NodeSerialOid> nodeSerialOids = new ArrayList<>();
        String query = "CALL GetNodeSerials(?)";
        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, nodeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    NodeSerialOid nodeSerialOid = new NodeSerialOid();
                    nodeSerialOid.setId(rs.getInt("id"));
                    nodeSerialOid.setIp(rs.getString("ip"));
                    nodeSerialOid.setName(rs.getString("name"));
                    nodeSerialOid.setSerialId(rs.getInt("serial_id"));
                    nodeSerialOid.setOnuid(rs.getInt("onuid"));
                    nodeSerialOid.setSerial(rs.getString("serial"));
                    nodeSerialOid.setOidDesc(rs.getString("oiddesc"));
                    nodeSerialOid.setNodeType(rs.getInt("nodetype"));
                    nodeSerialOid.setOidStatus(rs.getString("oidstatus"));
                    nodeSerialOid.setOidOnuRxPower(rs.getString("oidonurxpower"));
                    nodeSerialOid.setOidSerial(rs.getString("oidserial"));
                    nodeSerialOid.setOidOltRxPower(rs.getString("oidoltrxpower"));
                    nodeSerialOid.setOidTemperature(rs.getString("oidtemperature"));
                    nodeSerialOid.setOidLineProfile(rs.getString("oidlineprofile"));
                    nodeSerialOid.setOidDistance(rs.getString("oiddistance"));

                    nodeSerialOids.add(nodeSerialOid);
                }
            }
        } catch (SQLException e) {
            // System.err.println("Database error in getNodeSerialsOid: " + e.getMessage());
            logger.error("Database error in getNodeSerialsOid: " + e.getMessage());
        }
        return nodeSerialOids;
    }

    public void saveData(List<NodeSerialOid> nodeSerialOids) {
        // String sql = "INSERT INTO ont_data (id, rxpower, oltrxpower, distance,
        // temperature) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sql = "Update onuserial set rxpower=?, distance=?,oltrxpower=?,temperature=? where id=?";

        try (Connection conn = getDbConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (NodeSerialOid nso : nodeSerialOids) {
                pstmt.setInt(5, nso.getSerialId());
                pstmt.setFloat(1, nso.getOnuRxPower());
                pstmt.setFloat(2, nso.getOnuDistance());
                pstmt.setFloat(3, nso.getOnuOltRxPower());
                pstmt.setFloat(4, nso.getOnuTemperature());
                // pstmt.setFloat(6, nso.getOltRxPower());
                // pstmt.setFloat(7, nso.getDistance());
                // pstmt.setFloat(8, nso.getTemperature());

                pstmt.addBatch();
            }

            pstmt.executeBatch();
            // //////System.out.println("✅ " + nodeSerialOids.size() + " rows inserted
            // successfully into `ont_data`.");
            logger.info(nodeSerialOids.size() + " rows inserted successfully into `ont_data`.");
        } catch (SQLException e) {
            // System.err.println("❌ Failed to insert data into `ont_data`: " +
            // e.getMessage());
            logger.error("Database error in getNodeSerialsOid: " + e.toString());

        }
    }

    public void updateReachable(Node node) {
        String sql = "insert into pingstatus (nodeid,nodename,nodeip,isreachable) values (?,?,?,?)";
        try (Connection conn = getDbConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, node.getId());
            pstmt.setString(2, node.getName());
            pstmt.setString(3, node.getIp());
            pstmt.setInt(4, 0);

            int rowsAffected = pstmt.executeUpdate(); // ✅ Use executeUpdate() for INSERT/UPDATE/DELETE

            if (rowsAffected == 0) {
                System.err.println("⚠️ No rows updated for ID: " + node.getId());
            }

        } catch (SQLException e) {
            // System.err.println("❌ Failed to update reachable: " + e.getMessage());
            logger.error("Failed to update reachable: " + e.toString());

            // e.printStackTrace();
        }
    }


    public void updateOidNotFound(String oid,Node node) {
        String sql = "insert into oidnotfound (oid,nodename,nodeip,nodeid) values (?,?,?,?)";
        try (Connection conn = getDbConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, oid);
            pstmt.setString(2, node.getName());
            pstmt.setString(3, node.getIp());
            pstmt.setInt(4, node.getId());

            int rowsAffected = pstmt.executeUpdate(); // ✅ Use executeUpdate() for INSERT/UPDATE/DELETE

            if (rowsAffected == 0) {
                System.err.println("⚠️ No rows updated for ID: " + node.getId());
            }

        } catch (SQLException e) {
            // System.err.println("❌ Failed to update reachable: " + e.getMessage());
            logger.error("Failed to update OidNotFound: " + e.toString());

            // e.printStackTrace();
        }
    }

    public void insertOnuSerialDataFromView() {
        String sql = "{CALL InsertIntoOnuSerialData}"; // For SQL Server or MySQL

        try (Connection conn = getDbConnection();
                CallableStatement cstmt = conn.prepareCall(sql)) {

            boolean hadResults = cstmt.execute(); // Use execute() for stored procedures

            if (hadResults) {
                ////// System.out.println("✅ Data inserted successfully from view into
                ////// onuserialdata.");
            } else {
                ////// System.out.println("⚠️ Procedure executed, but no result set was
                ////// returned.");
            }

        } catch (SQLException e) {
            logger.error("❌ Failed to call stored procedure InsertIntoOnuSerialData: " + e.toString());
        }
    }

    public Parameter getSnmpConfig() {
        Parameter config = new Parameter(); // custom POJO
        String query = "SELECT threadpool, threadpernode, batchsize FROM parameter ORDER BY id DESC LIMIT 1";

        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                config.setThreadPool(rs.getInt("threadpool"));
                config.setThreadPerNode(rs.getInt("threadpernode"));
                config.setBatchSize(rs.getInt("batchsize"));
            }

        } catch (SQLException e) {
            logger.error("Database error in parameter: " + e.getMessage());
        }

        return config;
    }

    public void close() {
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
            ((com.zaxxer.hikari.HikariDataSource) dataSource).close();
        }
    }

    

    // public void updateOnuSerialRawBulk(List<OnuSerialWithOidClass> resultListOnuSerial,
    //         NodeType nodeType, Node node) throws SQLException {
    //     String queryOnuOidId = "SELECT id FROM onuoid WHERE nodetype=? AND oidserial LIKE ?";
    //     String queryOnuSerialRecords = "SELECT COUNT(*) AS rowscount FROM onuserialraw WHERE nodeid=? AND onuid=?";
    //     // String queryUpdate = "UPDATE onuserialraw SET serial=?, name=?, lastondate=?
    //     // WHERE nodeid=? AND onuid=?";
    //     // String queryInsert = "INSERT INTO onuserialraw(nodeid, onuid, serial,
    //     // name,lastondate) VALUES(?,?,?,?,?)";

    //     String queryUpdate = "UPDATE onuserialraw SET serial=?, name=? WHERE nodeid=? AND onuid=?";
    //     String queryInsert = "INSERT INTO onuserialraw(nodeid, onuid, serial, name) VALUES(?,?,?,?)";

    //     try (Connection conn = getDbConnection()) {
    //         for (OnuSerialWithOidClass onuSerialWithOidClass : resultListOnuSerial) {
    //             int onuOidId = -1;
    //             int rowsCount = 0;

    //             // 1. Get onuOidId
    //             try (PreparedStatement stmt = conn.prepareStatement(queryOnuOidId)) {
    //                 stmt.setInt(1, nodeType.getId());
    //                 stmt.setString(2, onuSerialWithOidClass.getOidOnuSerial());
    //                 // //////System.out.println(onuSerialWithOidClass.getOidOnuSerial());
    //                 try (ResultSet rs = stmt.executeQuery()) {
    //                     if (rs.next()) {
    //                         onuOidId = rs.getInt("id");
    //                     }
    //                 }
    //             }

    //             if (onuOidId == -1) {
    //                 logger.warn("No onuOidId found for OIDSerial {}",
    //                         onuSerialWithOidClass.getOidOnuSerial() + " : " + node.getIp() + ":" + node.getName());
    //                 continue; // skip this iteration
    //             }

    //             // 2. Check if record exists
    //             try (PreparedStatement stmt1 = conn.prepareStatement(queryOnuSerialRecords)) {
    //                 stmt1.setInt(1, node.getId());
    //                 stmt1.setInt(2, onuOidId);
    //                 try (ResultSet rs1 = stmt1.executeQuery()) {
    //                     if (rs1.next()) {
    //                         rowsCount = rs1.getInt("rowscount");
    //                     }
    //                 }
    //             }

    //             if (rowsCount > 0) {
    //                 // 3. Update
    //                 try (PreparedStatement stmtUpdate = conn.prepareStatement(queryUpdate)) {
    //                     stmtUpdate.setString(1, onuSerialWithOidClass.getOnuSerial().getSerial());
    //                     stmtUpdate.setString(2, onuSerialWithOidClass.getOnuSerial().getName());
    //                     // stmtUpdate.setTimestamp(3,onuSerialWithOidClass.getOnuSerial().getOnuLastOnline());
    //                     stmtUpdate.setInt(3, node.getId());
    //                     stmtUpdate.setInt(4, onuOidId);
    //                     stmtUpdate.executeUpdate();
    //                 }
    //             } else {
    //                 // 4. Insert
    //                 try (PreparedStatement stmtInsert = conn.prepareStatement(queryInsert)) {
    //                     stmtInsert.setInt(1, node.getId());
    //                     stmtInsert.setInt(2, onuOidId);
    //                     stmtInsert.setString(3, onuSerialWithOidClass.getOnuSerial().getSerial());
    //                     stmtInsert.setString(4, onuSerialWithOidClass.getOnuSerial().getName());
    //                     // stmtInsert.setTimestamp(5,onuSerialWithOidClass.getOnuSerial().getOnuLastOnline());
    //                     stmtInsert.executeUpdate();
    //                 }
    //             }
    //         }
    //         conn.close();
    //     } catch (SQLException e) {
    //         logger.error("Database error in updateOnuSerialRawBulk: {}", e.getMessage(), e);
    //         throw e;
    //     }
    // }



    public void updateOnuSerialRawBulk(List<OnuSerialWithOidClass> resultListOnuSerial,
                                   NodeType nodeType, Node node) throws SQLException {
    // Queries
    String queryOnuOidMap = "SELECT id, oidserial FROM onuoid WHERE nodetype=?";
    String queryUpsert = "INSERT INTO onuserialraw (nodeid, onuid, serial, name,lastondate) " +
                         "VALUES (?, ?, ?, ?,?) " +
                         "ON DUPLICATE KEY UPDATE serial=VALUES(serial), name=VALUES(name),lastondate=Values(lastondate)";

    try (Connection conn = getDbConnection()) {
        conn.setAutoCommit(false); // batch inside one transaction

        // 1. Preload onuOid map for this nodeType
        Map<String, Integer> onuOidMap = new HashMap<>();
        try (PreparedStatement stmt = conn.prepareStatement(queryOnuOidMap)) {
            stmt.setInt(1, nodeType.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    onuOidMap.put(rs.getString("oidserial"), rs.getInt("id"));
                }
            }
        }

        // 2. Prepare batch upsert
        try (PreparedStatement stmtUpsert = conn.prepareStatement(queryUpsert)) {
            int batchCount = 0;

            for (OnuSerialWithOidClass onuSerialWithOidClass : resultListOnuSerial) {
                Integer onuOidId = onuOidMap.get(onuSerialWithOidClass.getOidOnuSerial());

                if (onuOidId == null) {
                    updateOidNotFound(onuSerialWithOidClass.getOidOnuSerial(),node);
                    logger.warn("No onuOidId found for OIDSerial {} ({}:{})",
                            onuSerialWithOidClass.getOidOnuSerial(), node.getIp(), node.getName());
                    continue; // skip if no mapping found
                }

                // Set values for UPSERT
                stmtUpsert.setInt(1, node.getId());
                stmtUpsert.setInt(2, onuOidId);
                stmtUpsert.setString(3, onuSerialWithOidClass.getOnuSerial().getSerial());
                stmtUpsert.setString(4, onuSerialWithOidClass.getOnuSerial().getName());
                stmtUpsert.setTimestamp(5, onuSerialWithOidClass.getOnuSerial().getOnuLastOnline());
                stmtUpsert.addBatch();

                batchCount++;

                // Execute in chunks to avoid huge batches
                if (batchCount % 500 == 0) {
                    stmtUpsert.executeBatch();
                    batchCount = 0;
                }
            }

            // Execute remaining
            if (batchCount > 0) {
                stmtUpsert.executeBatch();
            }
        }

        conn.commit(); // commit once
        updateOnuserialFromOnuserialraw();

    } catch (SQLException e) {
        logger.error("Database error in updateOnuSerialRawBulk: {}", e.getMessage(), e);
        throw e;
    }
}

public void updateOnuserialFromOnuserialraw() {
        String sql = "{CALL sync_onuserial_from_onuserialraw}"; // For  MySQL

        try (Connection conn = getDbConnection();
                CallableStatement cstmt = conn.prepareCall(sql)) {

            boolean hadResults = cstmt.execute(); // Use execute() for stored procedures

            if (hadResults) {
                ////// System.out.println("✅ Data inserted successfully from view into
                ////// onuserialdata.");
                logger.info("✅ Data inserted successfully from view into onuserialdata.");
            } else {
                ////// System.out.println("⚠️ Procedure executed, but no result set was returned.");
                logger.info("⚠️ Procedure executed, but no result set was returned.");
            }

        } catch (SQLException e) {
            logger.error("❌ Failed to call stored procedure InsertIntoOnuSerialData: " + e.toString());
        }
    }


}
