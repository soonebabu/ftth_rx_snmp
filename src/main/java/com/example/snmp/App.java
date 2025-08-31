package com.example.snmp;

import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.example.snmp.Dao;
import com.example.snmp.NodeSerialOid;
import com.example.snmp.NodeType;
import com.example.snmp.Node;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

// For logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    // Logger instance for application-wide logging
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        String community = "public";  // Default SNMP community string
        long programStart = System.currentTimeMillis();  // Track start time
        logger.info("Application started");

        int nType = 0;       // Node type (used to filter nodes)
        String region = "all"; // Region filter, default is "all"
        String mode = "prod";  // Mode for DAO, default "prod"

        // Initialize DAO to interact with database
        Dao dao = new Dao(mode);
        Parameter parameter = dao.getSnmpConfig(); // Retrieve SNMP configuration

        // Parse command line arguments
        if (args.length >= 1) {
            try {
                // If the first argument is longer than 6 characters, it may be a special identifier
                if(args[0].length() > 6) {
                    logger.info("First argument is longer than 6 characters: {}", args[0]);        
                    Node node=dao.getNodeFromIpAndService(args[0], "ftth");  
                    NodeType nodeType=dao.getNodeTypeById(node.getType());          
                    (new OnuSerialFillerApp()).sendSnmpWalkAllOnus(node,nodeType,dao);
                    DataSourceSingleton.shutdownAll();
                    System.exit(0);
                }

                // Parse the first argument as node type
                nType = Integer.parseInt(args[0]);
                if (nType == 0) {
                    // If nType = 0, insert data from view to database and exit
                    (new Dao(mode)).insertOnuSerialDataFromView();
                    logger.info("Record Inserted From View to onuSerialData table successfully!!!");
                    System.exit(0);
                }
                else if(nType == 1) {
                    // nType = 1 triggers serial number scanning using OnuSerialFillerApp
                    (new OnuSerialFillerApp()).callableMain(mode);
                }
            } catch (Error e) {
                System.out.println("Invalid number for nType. Defaulting to 0.");
                logger.error("Error inside APP when argument length is greater or equal to 1 " + e.toString());
            }
        }

        // Parse optional region argument
        if (args.length >= 2) {
            region = args[1];
        }

        // Parse optional mode argument
        if (args.length >= 3) {
            mode = args[2];
        }

        

        // Create a fixed thread pool for processing nodes
        ExecutorService executor = Executors.newFixedThreadPool(parameter.getThreadPool());

        List<Future<?>> futures = new ArrayList<>();

        // Retrieve all nodes filtered by node type and region
        List<Node> nodes = dao.getNodes(nType, region);
        logInfo("Node Size:" + nodes.size());

        // Loop through each node and submit SNMP tasks to thread pool
        for (Node node : nodes) {
            // Check if node is reachable using ping
            if (!isReachable(node, dao)) {
                System.out.println("❌ " + node.getIp() + " is not reachable (ping failed)");
                logger.error(node.getIp() + " is not reachable (ping failed)");
                continue;
            }

            // Retrieve SNMP OIDs for the node
            List<NodeSerialOid> nodeSerialOids = dao.getNodeSerialsOid(node.getId());

            // Submit SNMP GET task to executor
            Future<?> future = executor.submit(() -> {
                try {
                    sendSnmpGet(node, community, nodeSerialOids, dao, parameter);
                } catch (IOException e) {
                    System.out.println("❌ Error querying " + node.getIp() + ": " + e.getMessage());
                }
            });
            futures.add(future);
        }

        // Wait for all submitted tasks to finish
        for (Future<?> f : futures) {
            try {
                f.get();  // Blocks until task completes
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Calculate total runtime
        long programEnd = System.currentTimeMillis();
        long totalms = programEnd - programStart;
        long totalMinutes = totalms / 1000 / 60;
        System.out.println("\n✅ Total time for all OLTs from main: " + totalMinutes + " minutes");

        executor.shutdown(); // Shutdown executor service
        dao.close();         // Close DAO resources (DB connection)
    }

    /**
     * Sends SNMP GET requests for a batch of NodeSerialOids for a given node
     * @param node Node to query
     * @param community SNMP community string
     * @param nodeSerialOids List of OIDs to query
     * @param dao DAO instance to save data
     * @param parameter SNMP configuration (batch size, threads per node)
     * @return List of NodeSerialOid with updated SNMP values
     * @throws IOException
     */
    private static List<NodeSerialOid> sendSnmpGet(Node node, String community, List<NodeSerialOid> nodeSerialOids,
            Dao dao, Parameter parameter)
            throws IOException {

        int totalOids = nodeSerialOids.size();
        int batchSize = parameter.getBatchSize();  // Max number of OIDs per SNMP GET request

        // Create a thread pool to process batches concurrently (max threads per node)
        ExecutorService batchExecutor = Executors.newFixedThreadPool(parameter.getThreadPerNode());
        List<Future<?>> batchFutures = new ArrayList<>();

        // Initialize SNMP transport and session
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        transport.listen();
        Snmp snmp = new Snmp(transport);

        // Prepare SNMP target
        CommunityTarget target = getCommunityTarget(node);

        // Split OIDs into batches and submit tasks
        for (int i = 0; i < totalOids; i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, totalOids);
            final List<NodeSerialOid> batch = nodeSerialOids.subList(start, end);

            Future<?> future = batchExecutor.submit(() -> {
                try {
                    // Prepare PDUs for different OIDs (Rx power, distance, OLT power, temperature)
                    PDU pduOnuRxPower = new PDU();
                    PDU pduOnuDistance = new PDU();
                    PDU pduOltRxPower = new PDU();
                    PDU pduTemperature = new PDU();

                    // Add OIDs to PDUs
                    for (NodeSerialOid oid : batch) {
                        pduOnuRxPower.add(new VariableBinding(new OID(oid.getOidOnuRxPower())));
                        pduOnuDistance.add(new VariableBinding(new OID(oid.getOidDistance())));
                        pduOltRxPower.add(new VariableBinding(new OID(oid.getOidOltRxPower())));
                        if (node.getType() <= 10) {
                            pduTemperature.add(new VariableBinding(new OID(oid.getOidTemperature())));
                        }
                    }

                    // Send SNMP GET requests
                    ResponseEvent response = snmp.get(pduOnuRxPower, target);
                    ResponseEvent responseDistance = snmp.get(pduOnuDistance, target);
                    ResponseEvent responseOltRxPower = snmp.get(pduOltRxPower, target);
                    ResponseEvent responseTemperature = null;
                    if (node.getType() <= 10) {
                        responseTemperature = snmp.get(pduTemperature, target);
                    }

                    // Process SNMP responses
                    if (response != null && response.getResponse() != null) {
                        PDU responsePdu = response.getResponse();
                        PDU responsePduDistance = responseDistance.getResponse();
                        PDU responsePduOltRxpower = responseOltRxPower.getResponse();
                        PDU responsePduTemperature = (node.getType() <= 10) ? responseTemperature.getResponse() : null;

                        int localIndex = 0, m = 0;
                        for (VariableBinding vb : responsePdu.getVariableBindings()) {
                            float rxPower = 0;
                            int distance = 0;
                            float oltRxPower = 0;
                            float temperature = 0;

                            // Retrieve corresponding OIDs from other PDUs
                            VariableBinding vbDistance = responsePduDistance.get(m);
                            VariableBinding vbOltRxpower = responsePduOltRxpower.get(m);
                            VariableBinding vbTemperature = (node.getType() <= 10) ? responsePduTemperature.get(m) : null;
                            m++;

                            try {
                                float rawValue = Float.parseFloat(vb.getVariable().toString());
                                distance = Integer.parseInt(vbDistance.getVariable().toString());
                                float rawOltRxPowerValue = Float.parseFloat(vbOltRxpower.getVariable().toString());

                                if (node.getType() > 10) {
                                    // For non-ZTE devices
                                    rxPower = (rawValue == 65535f) ? 0f : (rawValue * 0.002f - 30f);
                                    oltRxPower = (rawOltRxPowerValue == -80000f) ? 0f : (rawOltRxPowerValue / 1000f);
                                } else {
                                    // For ZTE devices
                                    float rawTemperature = Float.parseFloat(vbTemperature.getVariable().toString());
                                    rxPower = (rawValue > 0) ? 0f : (rawValue / 100f);
                                    oltRxPower = ((rawOltRxPowerValue / 100f) > 100f) ? 0f : (100f - rawOltRxPowerValue / 100f);
                                    temperature = (rawTemperature > 100f) ? 0f : rawTemperature;
                                }
                            } catch (NumberFormatException e) {
                                rxPower = 0f;  // Fallback if SNMP response is not a number
                            }

                            // Save SNMP values into the NodeSerialOid object
                            batch.get(localIndex).setOnuRxPower(rxPower);
                            batch.get(localIndex).setOnuDistance(distance);
                            batch.get(localIndex).setOnuOltRxPower(oltRxPower);
                            batch.get(localIndex).setOnuTemperature(temperature);

                            localIndex++;
                        }
                    } else {
                        logger.info("No SNMP response from " + node.getIp() + " for batch " + start + "-" + (end - 1));
                    }

                } catch (Exception e) {
                    logger.info("SNMP error on batch " + start + "-" + (end - 1) + ": " + e.getMessage());
                }
            });

            batchFutures.add(future);
        }

        // Wait for all batch tasks to complete
        for (Future<?> f : batchFutures) {
            try {
                f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        batchExecutor.shutdown(); // Shutdown batch executor
        snmp.close();             // Close SNMP session
        transport.close();        // Close transport

        // Save all retrieved SNMP data into database
        dao.saveData(nodeSerialOids);
        return nodeSerialOids;
    }

    /**
     * Checks if a node is reachable via ping
     */
    public static boolean isReachable(Node node, Dao dao) {
        boolean reachable = false;
        String ip = node.getIp();
        try {
            reachable = InetAddress.getByName(ip).isReachable(2000); // 2 seconds timeout
        } catch (Exception e) {
            dao.updateReachable(node); // Mark node as unreachable in DB
            logInfo("Error:: " + ip + ":::" + e.toString());
        }
        return reachable;
    }

    /**
     * Builds SNMP CommunityTarget object for a node
     */
    public static CommunityTarget getCommunityTarget(Node node) {
        CommunityTarget target = new CommunityTarget();
        String ipAddress = node.getIp();
        String community = node.getSnmpcommunity();
        target.setCommunity(new OctetString(community)); // SNMP community string
        target.setVersion(SnmpConstants.version2c);      // SNMP v2c
        target.setAddress(new UdpAddress(ipAddress + "/161")); // SNMP port 161
        target.setRetries(1);                             // Retry count
        target.setTimeout(10000);                         // Timeout in ms
        return target;
    }

    /**
     * Helper method to log information to console
     */
    private static void logInfo(String msg) {
        System.out.println("ℹ️ " + msg);
    }

}
