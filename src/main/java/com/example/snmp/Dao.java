package com.example.snmp;

import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO class handles all database operations for nodes, node types, ONUs, and SNMP parameters.
 */
public class Dao {

    private final DataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(Dao.class);

    /**
     * Constructor to initialize DAO with a mode for DataSource.
     * @param mode The mode used to obtain the DataSource (e.g., "prod", "dev").
     */
    public Dao(String mode) {
        this.dataSource = DataSourceSingleton.getDataSource(mode);
    }

    /**
     * Get a database connection from the DataSource.
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public Connection getDbConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**************************************************************
     * NODE TYPE SECTION
     **************************************************************/

    /**
     * Get all node types for a given service.
     * @param service Service name to filter node types
     * @return List of NodeType objects
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
            logger.error("Database error in getNodeTypes: " + e.getMessage());
        }
        return nodeTypes;
    }

    /**
     * Get a NodeType by its ID.
     * @param id NodeType ID
     * @return NodeType object or null if not found
     */
    public NodeType getNodeTypeById(int id) {
        NodeType nodeType = null;
        String query = "SELECT name, vendor, onuserial, onudesc, lastondate, onudistance, onurxpower FROM nodetype WHERE id = ?";
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
                    nodeType.setOidOnuLastOnDateTime(
                        lastOnDateStr != null && !lastOnDateStr.isEmpty() ? lastOnDateStr : "0000-00-00 00:00:00"
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Database error in getNodeTypeById: " + e.getMessage());
        }
        return nodeType;
    }

    /**************************************************************
     * NODE SECTION
     **************************************************************/

    /**
     * Get all nodes with service "ftth".
     * @return List of Node objects
     */
    public List<Node> getAllNodes() {
        List<Node> nodes = new ArrayList<>();
        String query = "SELECT * FROM node WHERE service like ?";
        try (Connection conn = getDbConnection();
             PreparedStatement stmt = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

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
            logger.error("Database error in getAllNodes: " + e.getMessage());
        }
        return nodes;
    }

    /**
     * Get nodes filtered by node type and region.
     * @param nodeType NodeType ID
     * @param region Region name or "all"
     * @return List of Node objects
     */
    public List<Node> getNodes(int nodeType, String region) {
        List<Node> nodes = new ArrayList<>();
        boolean isRegionSelected = !region.equalsIgnoreCase("all");
        String query = isRegionSelected ? "SELECT * FROM node WHERE type = ? AND region = ?" : "SELECT * FROM node WHERE type = ?";

        try (Connection conn = getDbConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, nodeType);
            if (isRegionSelected) stmt.setString(2, region);

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
            logger.error("Database error in getNodes: " + e.getMessage());
        }
        return nodes;
    }

    /**
     * Get nodes filtered by IP and Service.
     * @param ip OLT NODE IP
     * @param service ftth
     * @return Corresponding Node object
     */
    public Node getNodeFromIpAndService(String ip, String service) {        
        Node node=new Node();        
        String query ="SELECT * FROM node WHERE ip LIKE ? AND service LIKE ?";

        try (Connection conn = getDbConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, ip);
            stmt.setString(2, service);
            

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {                    
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
                }
            }
        } catch (SQLException e) {
            logger.error("Database error in getNoderomIpAndService: " + e.getMessage());
        }
        return node;
    }

    /**************************************************************
     * NODE SERIALS SECTION
     **************************************************************/

    /**
     * Get NodeSerialOid data by node ID via stored procedure.
     * @param nodeId Node ID
     * @return List of NodeSerialOid objects
     */
    public List<NodeSerialOid> getNodeSerialsOid(int nodeId) {
        List<NodeSerialOid> nodeSerialOids = new ArrayList<>();
        String query = "CALL GetNodeSerials(?)";

        try (Connection conn = getDbConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, nodeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    NodeSerialOid nso = new NodeSerialOid();
                    nso.setId(rs.getInt("id"));
                    nso.setIp(rs.getString("ip"));
                    nso.setName(rs.getString("name"));
                    nso.setSerialId(rs.getInt("serial_id"));
                    nso.setOnuid(rs.getInt("onuid"));
                    nso.setSerial(rs.getString("serial"));
                    nso.setOidDesc(rs.getString("oiddesc"));
                    nso.setNodeType(rs.getInt("nodetype"));
                    nso.setOidStatus(rs.getString("oidstatus"));
                    nso.setOidOnuRxPower(rs.getString("oidonurxpower"));
                    nso.setOidSerial(rs.getString("oidserial"));
                    nso.setOidOltRxPower(rs.getString("oidoltrxpower"));
                    nso.setOidTemperature(rs.getString("oidtemperature"));
                    nso.setOidLineProfile(rs.getString("oidlineprofile"));
                    nso.setOidDistance(rs.getString("oiddistance"));
                    nodeSerialOids.add(nso);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error in getNodeSerialsOid: " + e.getMessage());
        }
        return nodeSerialOids;
    }

    /**************************************************************
     * BULK INSERT / UPDATE SECTION
     **************************************************************/

    /**
     * Save list of NodeSerialOid updates to onuserial table.
     * @param nodeSerialOids List of NodeSerialOid objects containing data to save
     */
    public void saveData(List<NodeSerialOid> nodeSerialOids) {
        String sql = "UPDATE onuserial SET rxpower=?, distance=?, oltrxpower=?, temperature=? WHERE id=?";
        try (Connection conn = getDbConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (NodeSerialOid nso : nodeSerialOids) {
                pstmt.setFloat(1, nso.getOnuRxPower());
                pstmt.setFloat(2, nso.getOnuDistance());
                pstmt.setFloat(3, nso.getOnuOltRxPower());
                pstmt.setFloat(4, nso.getOnuTemperature());
                pstmt.setInt(5, nso.getSerialId());
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            logger.info(nodeSerialOids.size() + " rows updated successfully into `onuserial`.");
        } catch (SQLException e) {
            logger.error("Database error in saveData: " + e.toString());
        }
    }

    /**
     * Insert a ping status entry for a node.
     * @param node Node object
     */
    public void updateReachable(Node node) {
        String sql = "INSERT INTO pingstatus (nodeid,nodename,nodeip,isreachable) VALUES (?,?,?,?)";
        try (Connection conn = getDbConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, node.getId());
            pstmt.setString(2, node.getName());
            pstmt.setString(3, node.getIp());
            pstmt.setInt(4, 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update reachable: " + e.toString());
        }
    }

    /**
     * Insert a record for OIDs not found in scanning.
     * @param oid OID string
     * @param node Node object
     */
    public void updateOidNotFound(String oid, Node node) {
        String sql = "INSERT INTO oidnotfound (oid,nodename,nodeip,nodeid) VALUES (?,?,?,?)";
        try (Connection conn = getDbConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, oid);
            pstmt.setString(2, node.getName());
            pstmt.setString(3, node.getIp());
            pstmt.setInt(4, node.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update OidNotFound: " + e.toString());
        }
    }

    /**************************************************************
     * STORED PROCEDURE SECTION
     **************************************************************/

    /**
     * Call stored procedure to insert ONU serial data from a view.
     */
    public void insertOnuSerialDataFromView() {
        String sql = "{CALL InsertIntoOnuSerialData}";
        try (Connection conn = getDbConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.execute();
            logger.info("Stored procedure InsertIntoOnuSerialData executed.");
        } catch (SQLException e) {
            logger.error("Failed to call stored procedure InsertIntoOnuSerialData: " + e.toString());
        }
    }

    /**
     * Get SNMP configuration parameters.
     * @return Parameter object containing thread pool size, thread per node, and batch size
     */
    public Parameter getSnmpConfig() {
        Parameter config = new Parameter();
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
            logger.error("Database error in getSnmpConfig: " + e.getMessage());
        }
        return config;
    }

    /**
     * Close the DataSource if it is HikariCP.
     */
    public void close() {
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
            ((com.zaxxer.hikari.HikariDataSource) dataSource).close();
        }
    }

    /**************************************************************
     * BULK SNMP UPDATE SECTION
     **************************************************************/

    /**
     * Bulk insert/update ONU serial data to onuserialraw.
     * @param resultListOnuSerial List of OnuSerialWithOidClass objects containing scanned data
     * @param nodeType NodeType object of the node
     * @param node Node object
     * @throws SQLException if a database error occurs
     */
    public void updateOnuSerialRawBulk(List<OnuSerialWithOidClass> resultListOnuSerial,
                                       NodeType nodeType, Node node) throws SQLException {

        String queryOnuOidMap = "SELECT id, oidserial FROM onuoid WHERE nodetype=?";
        String queryUpsert = "INSERT INTO onuserialraw (nodeid, onuid, serial, name,lastondate) " +
                             "VALUES (?, ?, ?, ?,?) " +
                             "ON DUPLICATE KEY UPDATE serial=VALUES(serial), name=VALUES(name), lastondate=VALUES(lastondate)";

        try (Connection conn = getDbConnection()) {
            conn.setAutoCommit(false);

            Map<String, Integer> onuOidMap = new HashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement(queryOnuOidMap)) {
                stmt.setInt(1, nodeType.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        onuOidMap.put(rs.getString("oidserial"), rs.getInt("id"));
                    }
                }
            }

            try (PreparedStatement stmtUpsert = conn.prepareStatement(queryUpsert)) {
                int batchCount = 0;
                for (OnuSerialWithOidClass onuSerialWithOidClass : resultListOnuSerial) {
                    Integer onuOidId = onuOidMap.get(onuSerialWithOidClass.getOidOnuSerial());
                    if (onuOidId == null) {
                        updateOidNotFound(onuSerialWithOidClass.getOidOnuSerial(), node);
                        continue;
                    }

                    stmtUpsert.setInt(1, node.getId());
                    stmtUpsert.setInt(2, onuOidId);
                    stmtUpsert.setString(3, onuSerialWithOidClass.getOnuSerial().getSerial());
                    stmtUpsert.setString(4, onuSerialWithOidClass.getOnuSerial().getName());
                    stmtUpsert.setTimestamp(5, onuSerialWithOidClass.getOnuSerial().getOnuLastOnline());
                    stmtUpsert.addBatch();
                    batchCount++;

                    if (batchCount % 500 == 0) stmtUpsert.executeBatch();
                }
                if (batchCount > 0) stmtUpsert.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            logger.error("Database error in updateOnuSerialRawBulk: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Sync data from onuserialraw table to onuserial table using stored procedure.
     */
    public void updateOnuserialFromOnuserialraw() {
        String sql = "{CALL sync_onuserial_from_onuserialraw}";
        try (Connection conn = getDbConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {
            cstmt.execute();
            logger.info("Data synced from onuserialraw to onuserial.");
        } catch (SQLException e) {
            logger.error("Failed to call stored procedure sync_onuserial_from_onuserialraw: " + e.toString());
        }
    }
}
