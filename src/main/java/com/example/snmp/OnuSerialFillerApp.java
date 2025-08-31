package com.example.snmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.sql.SQLException;
import java.sql.Timestamp;

// For logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

/**
 * This class handles SNMP Walk operations for all ONUs under a node.
 * It retrieves ONU serials, last online time, and descriptions, then saves to the database.
 */
public class OnuSerialFillerApp {

    // Logger for logging information and errors
    private final Logger logger = LoggerFactory.getLogger(App.class);

    // Helper class instance for utility methods (ping, SNMP date parsing, MAC formatting)
    private HelperClass hc = new HelperClass();

    /**
     * Iterates through nodes, checks reachability, and performs SNMP walk to fill ONU serials.
     * Multithreading is used to parallelize across multiple nodes.
     */
    public void fillOnuSerial(Dao dao, int nType, String region) {

        // Retrieve nodes filtered by node type and region
        List<Node> nodes = dao.getNodes(nType, region);
        logger.info("Total Nodes = " + nodes.size());

        // Limit threads to 50 for concurrent SNMP walks
        int threadpool = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadpool);
        List<Future<?>> futures = new ArrayList<>();

        // Retrieve NodeType object for current node type
        NodeType nodeType = dao.getNodeTypeById(nType);

        for (Node node : nodes) {

            // Skip nodes that are not reachable via ping
            if (!hc.isReachable(node, dao)) {
                logger.error(node.getIp() + " is not reachable (ping failed)");
                continue;
            }

            // Submit SNMP walk task for the node
            Future<?> future = executor.submit(() -> {
                try {
                    sendSnmpWalkAllOnus(node, nodeType, dao);
                } catch (Exception e) {
                    logger.error("❌ Error querying " + node.getIp() + ": " + e.getMessage());
                }
            });
            futures.add(future);
        }

        // Wait for all node tasks to finish
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Shutdown executor and update final ONUs in database
        executor.shutdown();
        dao.updateOnuserialFromOnuserialraw();
    }

    /**
     * Performs SNMP walk on a single node to retrieve all ONUs' information.
     * Retrieves description, last online timestamp, and serial for each ONU.
     */
    public void sendSnmpWalkAllOnus(Node node, NodeType nodeType, Dao dao) {

        List<OnuSerialWithOidClass> resultListOnuSerial = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        int counter = 0;

        TransportMapping<UdpAddress> transport = null;
        Snmp snmp = null;

        try {
            // Initialize SNMP transport and session
            transport = new DefaultUdpTransportMapping();
            transport.listen();
            snmp = new Snmp(transport);

            // Prepare SNMP target for the node
            CommunityTarget target = hc.getCommunityTarget(node);

            // Create OIDs for ONU description, last online, and serial
            OID rootOIDOnuDescription = new OID(nodeType.getOidOnuDescription());
            OID rootOIDOnuLastOnline = new OID(nodeType.getOidOnuLastOnDateTime());
            OID rootOIDSerial = new OID(nodeType.getOidOnuSerial());

            // SNMP tree utility to walk subtree
            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());

            // Retrieve all SNMP data for the node
            List<TreeEvent> events = treeUtils.getSubtree(target, rootOIDOnuDescription);
            List<TreeEvent> eventsLastOn = treeUtils.getSubtree(target, rootOIDOnuLastOnline);
            List<TreeEvent> eventsOnuSerial = treeUtils.getSubtree(target, rootOIDSerial);

            // Check if SNMP response is empty
            if (events == null || events.isEmpty()) {
                System.err.println("No SNMP response received from " + node.getIp());
                logger.info("No SNMP response received from " + node.getIp());
            } else {
                int i = 0;

                for (TreeEvent event : events) {
                    if (event == null || event.isError()) {
                        System.err.println("SNMP error from " + node.getIp() + ": " + event.getErrorMessage());
                        continue;
                    }

                    TreeEvent eventOnuLastOn = eventsLastOn.get(i);
                    TreeEvent eventOnuSerial = eventsOnuSerial.get(i);

                    VariableBinding[] vbs = event.getVariableBindings();
                    VariableBinding[] vbsOnuLastOn = eventOnuLastOn.getVariableBindings();
                    VariableBinding[] vbsOnuSerial = eventOnuSerial.getVariableBindings();

                    if (vbs == null)
                        continue;

                    int j = 0;

                    for (VariableBinding vb : vbs) {
                        VariableBinding vbOnuLastOn = vbsOnuLastOn[j];
                        VariableBinding vbOnuSerial = vbsOnuSerial[j];

                        // Extract ONU description safely
                        String rawValue = (vb != null && vb.getVariable() != null
                                && !(vb.getVariable() instanceof Null))
                                        ? vb.getVariable().toString()
                                        : "";

                        // Extract last online timestamp safely
                        String rawValueOnuLastOn = (vbOnuLastOn != null && vbOnuLastOn.getVariable() != null
                                && !(vbOnuLastOn.getVariable() instanceof Null))
                                ? vbOnuLastOn.getVariable().toString()
                                : "";

                        Timestamp onuLastOnline = hc.parseSnmpDate(rawValueOnuLastOn);

                        // Extract ONU serial safely
                        String rawValueOnuSerial = (vbOnuSerial != null && vbOnuSerial.getVariable() != null
                                && !(vbOnuSerial.getVariable() instanceof Null))
                                        ? vbOnuSerial.getVariable().toString()
                                        : "";

                        // Create ONU serial objects
                        OnuSerialWithOidClass onuSerialWithOidClass = new OnuSerialWithOidClass();
                        OnuSerial onuSerial = new OnuSerial();
                        onuSerial.setName(rawValue);
                        onuSerial.setSerial(hc.macToCustomFormat(rawValueOnuSerial));
                        onuSerial.setOnuLastOnline(onuLastOnline);

                        onuSerialWithOidClass.setOnuSerial(onuSerial);
                        onuSerialWithOidClass
                                .setOidOnuSerial(vbOnuSerial != null ? vbOnuSerial.getOid().toString() : "");

                        // Add to result list
                        resultListOnuSerial.add(onuSerialWithOidClass);

                        j++;
                        counter++;
                    }
                    i++;
                }
            }

        } catch (IOException e) {
            System.err.println("SNMP walk failed for node " + node.getIp() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close SNMP session and transport safely
            try {
                if (snmp != null) snmp.close();
                if (transport != null) transport.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Bulk update ONU serials into database
        try {
            dao.updateOnuSerialRawBulk(resultListOnuSerial, nodeType, node);
        } catch (SQLException e) {
            logger.error("Error In Updating OnuSerialRaw table!!! " + e.toString());
        }

        long endTime = System.currentTimeMillis();
        double durationInSeconds = (endTime - startTime) / 1000.0;
        // System.out.println("Time taken: " + durationInSeconds + " seconds");
    }

    /**
     * Standalone main method for testing or running the app independently.
     * Executes SNMP walk for multiple node types in parallel.
     */
    public static void main(String[] args) {
        try {
            String mode = "prod";
            long startTime = System.currentTimeMillis();

            // Define node types to scan
            int[] nodetypes = { 9, 10, 12, 17, 18 };

            // Executor to process multiple node types concurrently
            ExecutorService executor1 = Executors.newFixedThreadPool(5);
            List<Future<?>> futures1 = new ArrayList<>();

            for (int nodetype : nodetypes) {
                Future<?> future1 = executor1.submit(() -> {
                    try {
                        (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"), nodetype, "all");
                    } catch (Exception e) {
                        System.out.println("❌ Error querying " + ": " + e.getMessage());
                    }
                });
                futures1.add(future1);
            }

            // Wait for all node type tasks to finish
            for (Future<?> f : futures1) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            executor1.shutdown();

            long endTime = System.currentTimeMillis();
            double durationInSeconds = (endTime - startTime) / 1000.0;
            System.out.println("Total duration time taken: " + durationInSeconds + " seconds");
        } catch(Exception e) {
            System.out.println("Error in loading in main()!!! "+e.toString());
        }
    }

    /**
     * Callable-friendly method to execute SNMP walk in multithreaded context.
     * Similar to main() but can be invoked from other threads.
     */
    public void callableMain(String mode) {
        try {
            long startTime = System.currentTimeMillis();

            Dao d = new Dao(mode);
            int[] nodetypes = { 9, 10, 12, 17, 18 };

            ExecutorService executor1 = Executors.newFixedThreadPool(5);
            List<Future<?>> futures1 = new ArrayList<>();

            for (int nodetype : nodetypes) {
                Future<?> future1 = executor1.submit(() -> {
                    try {
                        (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"), nodetype, "all");
                    } catch (Exception e) {
                        System.out.println("❌ Error querying " + ": " + e.getMessage());
                    }
                });
                futures1.add(future1);
            }

            for (Future<?> f : futures1) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            executor1.shutdown();

            long endTime = System.currentTimeMillis();
            double durationInSeconds = (endTime - startTime) / 1000.0;
            System.out.println("Total duration time taken: " + durationInSeconds + " seconds");
        } catch(Exception e) {
            System.out.println("Error in loading in main()!!! "+e.toString());
        }
    }

}
