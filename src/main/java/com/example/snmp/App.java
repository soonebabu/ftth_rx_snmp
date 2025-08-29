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

//for logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    // Global instance
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        String community = "public";
        long programStart = System.currentTimeMillis();
        logger.info("Application started");

        int nType = 0;
        String region = "all";
        String mode = "prod";

        if (args.length >= 1) {
            try {

                if(args[0].length()>6)
                {
                    logger.info("First argument is longer than 6 characters: {}", args[0]);                    
                    // (new OnuSerialFillerApp()).sendSnmpWalkAllOnus(node,nType);

                }

                nType = Integer.parseInt(args[0]);
                if (nType == 0) {
                    (new Dao(mode)).insertOnuSerialDataFromView();
                    logger.info("Record Inserted From View to onuSerialData table successfully!!!");
                    System.exit(0);
                }
                else if(nType == 1)//for serial number scanning
                {
                    (new OnuSerialFillerApp()).callableMain(mode);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number for nType. Defaulting to 0.");
                logger.error("Invalid number for nType. Defaulting to 0." + e.toString());
            }
        }

        if (args.length >= 2) {
            region = args[1];
        }

        if (args.length >= 3) {
            mode = args[2];
        }

        Dao dao = new Dao(mode);
        Parameter parameter = dao.getSnmpConfig();

        ExecutorService executor = Executors.newFixedThreadPool(parameter.getThreadPool());

        List<Future<?>> futures = new ArrayList<>();

        // retrieve all nodes for each nodeTypes and loop throut it
        List<Node> nodes = dao.getNodes(nType, region);
        logInfo("Node Size:" + nodes.size());

        // Thread-safe list to collect results from all threads
        // List<String> resultList = Collections.synchronizedList(new ArrayList<>());

        for (Node node : nodes) {
            if (!isReachable(node, dao)) {
                System.out.println("❌ " + node.getIp() + " is not reachable (ping failed)");
                logger.error(node.getIp() + " is not reachable (ping failed)");
                continue;

            }

            // get NodeSerialOids for that node.
            List<NodeSerialOid> nodeSerialOids = dao.getNodeSerialsOid(node.getId());
            Future<?> future = executor.submit(() -> {
                try {

                    sendSnmpGet(node, community, nodeSerialOids, dao, parameter);

                } catch (IOException e) {
                    System.out.println("❌ Error querying " + node.getIp() + ": " + e.getMessage());
                }
            });
            futures.add(future);
        }

        // Wait for all threads to finish
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        long programEnd = System.currentTimeMillis();
        long totalms = programEnd - programStart;
        long totalMinutes = totalms / 1000 / 60;
        System.out.println("\n✅ Total time for all OLTs from main: " + totalMinutes + " minutes");

        // dao.insertOnuSerialDataFromView();

        executor.shutdown();

        dao.close();

    }

    private static List<NodeSerialOid> sendSnmpGet(Node node, String community, List<NodeSerialOid> nodeSerialOids,
            Dao dao, Parameter parameter)
            throws IOException {

        int totalOids = nodeSerialOids.size();
        int batchSize = parameter.getBatchSize();

        // Create a thread pool to parallelize batches (max 10 parallel batches per
        // node)
        ExecutorService batchExecutor = Executors.newFixedThreadPool(parameter.getThreadPerNode());
        List<Future<?>> batchFutures = new ArrayList<>();

        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        transport.listen();
        Snmp snmp = new Snmp(transport);
        CommunityTarget target = getCommunityTarget(node);

        for (int i = 0; i < totalOids; i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, totalOids);
            final List<NodeSerialOid> batch = nodeSerialOids.subList(start, end);

            // Submit a task to process each batch in parallel
            Future<?> future = batchExecutor.submit(() -> {
                try {
                    PDU pduOnuRxPower = new PDU();
                    PDU pduOnuDistance = new PDU();
                    PDU pduOltRxPower = new PDU();
                    PDU pduTemperature = new PDU();

                    for (NodeSerialOid oid : batch) {
                        pduOnuRxPower.add(new VariableBinding(new OID(oid.getOidOnuRxPower())));
                        pduOnuDistance.add(new VariableBinding(new OID(oid.getOidDistance())));
                        pduOltRxPower.add(new VariableBinding(new OID(oid.getOidOltRxPower())));
                        if (node.getType() <= 10) {
                            pduTemperature.add(new VariableBinding(new OID(oid.getOidTemperature())));

                        }

                    }

                    ResponseEvent response = snmp.get(pduOnuRxPower, target);
                    ResponseEvent responseDistance = snmp.get(pduOnuDistance, target);
                    ResponseEvent responseOltRxPower = snmp.get(pduOltRxPower, target);
                    ResponseEvent responseTemperature = null;
                    if (node.getType() <= 10) {
                        responseTemperature = snmp.get(pduTemperature, target);

                    }

                    if (response != null && response.getResponse() != null) {
                        PDU responsePdu = response.getResponse();
                        PDU responsePduDistance = responseDistance.getResponse();
                        PDU responsePduOltRxpower = responseOltRxPower.getResponse();
                        PDU responsePduTemperature = null;

                        if (node.getType() <= 10) {
                            responsePduTemperature = responseTemperature.getResponse();
                        }

                        int localIndex = 0, m = 0;
                        for (VariableBinding vb : responsePdu.getVariableBindings()) {
                            float rxPower = 0;
                            int distance = 0;
                            float oltRxPower = 0;
                            float temperature = 0;

                            VariableBinding vbDistance = responsePduDistance.get(m);
                            VariableBinding vbOltRxpower = responsePduOltRxpower.get(m);
                            VariableBinding vbTemperature = null;
                            if (node.getType() <= 10) {
                                vbTemperature = responsePduTemperature.get(m);
                            }
                            m++;
                            try {
                                float rawValue = Float.parseFloat(vb.getVariable().toString());
                                distance = Integer.parseInt(vbDistance.getVariable().toString());
                                float rawOltRxPowerValue = Float.parseFloat(vbOltRxpower.getVariable().toString());

                                if (node.getType() > 10) {
                                    rxPower = (rawValue == 65535f) ? 0f : (rawValue * 0.002f - 30f);
                                    oltRxPower = (rawOltRxPowerValue == -80000f) ? 0f : (rawOltRxPowerValue / 1000f);
                                    // temperature=0; //since no oid of temperature for zte olt

                                } else {
                                    float rawTemperature = Float.parseFloat(vbTemperature.getVariable().toString());
                                    rxPower = (rawValue > 0) ? 0f : (rawValue / 100f);
                                    oltRxPower = ((rawOltRxPowerValue / 100f) > 100f) ? 0f
                                            : (100f - rawOltRxPowerValue / 100f);
                                    temperature = (rawTemperature > 100f) ? 0f : rawTemperature;
                                }
                            } catch (NumberFormatException e) {
                                rxPower = 0f;
                            }
                            batch.get(localIndex).setOnuRxPower(rxPower);
                            batch.get(localIndex).setOnuDistance(distance);
                            batch.get(localIndex).setOnuOltRxPower(oltRxPower);
                            batch.get(localIndex).setOnuTemperature(temperature);

                            // logInfo(node.getType() + ":::" + batch.get(localIndex).getId() +":::" +
                            // batch.get(localIndex).getName() + ":::" +
                            // batch.get(localIndex).getSerial() + ":::" + rxPower);
                            localIndex++;
                        }
                    } else {
                        // logInfo("⚠ No SNMP response from " + node.getIp() + " for batch " + start +
                        // "-" + (end - 1));
                        logger.info("No SNMP response from " + node.getIp() + " for batch " + start + "-" + (end - 1));
                    }

                } catch (Exception e) {
                    // logInfo("❌ SNMP error on batch " + start + "-" + (end - 1) + ": " +
                    // e.getMessage());
                    logger.info(" SNMP error on batch \" + start + \"-\" + (end - 1) + \": \" + e.getMessage()");
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

        batchExecutor.shutdown();
        snmp.close();
        transport.close();

        // Save all updated Rx power values to DB
        dao.saveData(nodeSerialOids);
        return nodeSerialOids;
    }

    public static boolean isReachable(Node node, Dao dao) {
        boolean reachable = false;
        String ip = node.getIp();
        try {
            reachable = InetAddress.getByName(ip).isReachable(2000);
        } catch (Exception e) {
            dao.updateReachable(node);
            logInfo("Error:: " + ip + ":::" + e.toString());
        }
        return reachable;
    }

    public static CommunityTarget getCommunityTarget(Node node) {
        CommunityTarget target = new CommunityTarget();
        String ipAddress = node.getIp();
        String community = node.getSnmpcommunity();
        target.setCommunity(new OctetString(community));
        target.setVersion(SnmpConstants.version2c);
        target.setAddress(new UdpAddress(ipAddress + "/161"));
        target.setRetries(1);
        target.setTimeout(10000);
        return target;
    }

    private static void logInfo(String msg) {
        System.out.println("ℹ️ " + msg);
    }

}
