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

//for logging
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

public class OnuSerialFillerApp {

    // Global instance
    private final Logger logger = LoggerFactory.getLogger(App.class);
    private HelperClass hc = new HelperClass();

    public void fillOnuSerial(Dao dao, int nType, String region) {

        List<Node> nodes = dao.getNodes(nType, region);
        logger.info("Total Nodes = " + nodes.size());
        // System.out.println(region);
        // int threadpool=nodes.size();
        int threadpool = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadpool);

        List<Future<?>> futures = new ArrayList<>();

        NodeType nodeType = dao.getNodeTypeById(nType);

        for (Node node : nodes) {

            if (!hc.isReachable(node, dao)) {
                logger.error(node.getIp() + " is not reachable (ping failed)");
                continue;
            }

            Future<?> future = executor.submit(() -> {
                try {

                    sendSnmpWalkAllOnus(node, nodeType, dao);

                } catch (Exception e) {
                    // System.out.println("❌ Error querying " + node.getIp() + ": " +
                    // e.getMessage());
                    logger.error("❌ Error querying " + node.getIp() + ": " + e.getMessage());
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

        executor.shutdown();
        dao.updateOnuserialFromOnuserialraw();

        // dao.close();

    }

    public void sendSnmpWalkAllOnus(Node node, NodeType nodeType, Dao dao) {

        HelperClass hc = new HelperClass();
        List<OnuSerialWithOidClass> resultListOnuSerial = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        int counter = 0;

        TransportMapping<UdpAddress> transport = null;
        Snmp snmp = null;

        try {
            transport = new DefaultUdpTransportMapping();
            transport.listen();

            snmp = new Snmp(transport);

            CommunityTarget target = hc.getCommunityTarget(node);
            OID rootOIDOnuDescription = new OID(nodeType.getOidOnuDescription());
            OID rootOIDOnuLastOnline = new OID(nodeType.getOidOnuLastOnDateTime());
            OID rootOIDSerial = new OID(nodeType.getOidOnuSerial());

            // System.out.println("Starting SNMP walk on " + node.getIp() + " for OID " +
            // rootOIDOnuDescription);

            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            List<TreeEvent> events = treeUtils.getSubtree(target, rootOIDOnuDescription);
            List<TreeEvent> eventsLastOn = treeUtils.getSubtree(target,rootOIDOnuLastOnline);
            List<TreeEvent> eventsOnuSerial = treeUtils.getSubtree(target, rootOIDSerial);
            // List<TreeEvent> eventsOnuDistance = treeUtils.getSubtree(target,
            // rootOIDOnuDistance);
            // List<TreeEvent> eventsOnuRxPower = treeUtils.getSubtree(target,
            // rootOIDOnuRxPower);

            if (events == null || events.isEmpty()) {
                System.err.println("No SNMP response received from " + node.getIp());
                logger.info("No SNMP response received from " + node.getIp());
            } else {
                int i = 0;
                TreeEvent eventOnuLastOn = null;
                TreeEvent eventOnuSerial = null;

                for (TreeEvent event : events) {
                    if (event == null || event.isError()) {
                        System.err.println("SNMP error from " + node.getIp() + ": " + event.getErrorMessage());
                        continue;
                    }

                    eventOnuLastOn = eventsLastOn.get(i);
                    eventOnuSerial = eventsOnuSerial.get(i);
                    // eventOnuDistance = eventsOnuDistance.get(i);
                    // eventOnuRxPower = eventsOnuRxPower.get(i);

                    VariableBinding[] vbs = event.getVariableBindings();

                    VariableBinding[] vbsOnuLastOn = eventOnuLastOn.getVariableBindings();
                    VariableBinding[] vbsOnuSerial = eventOnuSerial.getVariableBindings();
                    // VariableBinding[] vbsOnuDistance = eventOnuDistance.getVariableBindings();
                    // VariableBinding[] vbsOnuRxPower = eventOnuRxPower.getVariableBindings();
                    if (vbs == null)
                        continue;

                    int j = 0;
                    VariableBinding vbOnuLastOn = null;
                    VariableBinding vbOnuSerial = null;
                    // VariableBinding vbOnuDistance = null;
                    // VariableBinding vbOnuRxPower = null;

                    for (VariableBinding vb : vbs) {
                        vbOnuLastOn = vbsOnuLastOn[j];
                        vbOnuSerial = vbsOnuSerial[j];
                        // vbOnuDistance = vbsOnuDistance[j];
                        // vbOnuRxPower = vbsOnuRxPower[j];

                        // String rawValue = vb.getVariable().toString();

                        // String rawValueOnuLastOn = vbOnuLastOn.getVariable().toString();

                        // String rawValueOnuSerial = vbOnuSerial.getVariable().toString();

                        String rawValue = (vb != null && vb.getVariable() != null
                                && !(vb.getVariable() instanceof Null))
                                        ? vb.getVariable().toString()
                                        : "";

                        String rawValueOnuLastOn = (vbOnuLastOn != null && vbOnuLastOn.getVariable()
                        != null && !(vbOnuLastOn.getVariable() instanceof Null))
                        ? vbOnuLastOn.getVariable().toString() : "";

                        Timestamp onuLastOnline=hc.parseSnmpDate(rawValueOnuLastOn);

                        // System.out.println(rawValueOnuLastOn);

                        String rawValueOnuSerial = (vbOnuSerial != null && vbOnuSerial.getVariable() != null
                                && !(vbOnuSerial.getVariable() instanceof Null))
                                        ? vbOnuSerial.getVariable().toString()
                                        : "";

                        // String rawValueOnuDistance = vbOnuDistance.getVariable().toString();
                        // String rawValueOnuRxPower = vbOnuRxPower.getVariable().toString();

                        // String formattedValue = macToCustomFormat(rawValue);
                        // //System.out.println(++counter + " : " + vb.getOid() + " = " +
                        // formattedValue);
                        OnuSerialWithOidClass onuSerialWithOidClass = new OnuSerialWithOidClass();
                        OnuSerial onuSerial = new OnuSerial();
                        onuSerial.setName(rawValue);
                        onuSerial.setSerial(hc.macToCustomFormat(rawValueOnuSerial));

                        onuSerial.setOnuLastOnline(onuLastOnline);

                        // onuSerial.setOnuLastOnline(hc.parseOnuTimestamp(rawValueOnuLastOn));
                        // safely parse timestamp
                        // if (!rawValueOnuLastOn.isEmpty()) {
                        // try {
                        // onuSerial.setOnuLastOnline(hc.parseOnuTimestamp(rawValueOnuLastOn));
                        // } catch (Exception e) {
                        // onuSerial.setOnuLastOnline(null); // fallback if parsing fails
                        // logger.warn("Invalid ONU last online value: {}", rawValueOnuLastOn);
                        // }
                        // } else {
                        // onuSerial.setOnuLastOnline(null);
                        // }
                        onuSerialWithOidClass.setOnuSerial(onuSerial);
                        // onuSerialWithOidClass.setOidOnuSerial(vbOnuSerial.getOid().toString());
                        onuSerialWithOidClass
                                .setOidOnuSerial(vbOnuSerial != null ? vbOnuSerial.getOid().toString() : "");

                        resultListOnuSerial.add(onuSerialWithOidClass);

                        ++counter;
                        System.out.println(counter + " : " + vb.getOid() + " = " + rawValue);
                        System.out.println(
                        counter + " : " + vbOnuSerial.getOid() + " = "
                        + hc.macToCustomFormat(rawValueOnuSerial));
                        // System.out
                        // .println(counter + " : " +node.getName()+" : " + vbOnuLastOn.getOid() + " = "
                        // + rawValueOnuLastOn);

                        // //System.out.println(counter + " : " + vbOnuDistance.getOid() + " = "
                        // + rawValueOnuDistance);
                        // //System.out.println(counter + " : " + vbOnuRxPower.getOid() + " = "
                        // + rawValueOnuRxPower);
                        // //System.out.println(++counter + " : " + vb.getOid() + " = " + rawValue);

                        j++;
                    }
                    i++;

                }
            }

        } catch (IOException e) {
            System.err.println("SNMP walk failed for node " + node.getIp() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (snmp != null)
                    snmp.close();
                if (transport != null)
                    transport.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            dao.updateOnuSerialRawBulk(resultListOnuSerial, nodeType, node);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            logger.error("Error In Updating OnuSerialRaw table!!! " + e.toString());
        }
        long endTime = System.currentTimeMillis();
        double durationInSeconds = (endTime - startTime) / 1000.0;
        // System.out.println("Time taken: " + durationInSeconds + " seconds");
    }

    public static void main(String[] args) {
        try {
            String mode = "prod";
            long startTime = System.currentTimeMillis();

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

            // (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"),17, "all");
            (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"), 12, "all");
            // (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"),9, "all");
            // (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"),10, "all");
            // (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"),18, "all");

            long endTime = System.currentTimeMillis();
            double durationInSeconds = (endTime - startTime) / 1000.0;
            System.out.println("Total duration time taken: " + durationInSeconds + "seconds");
        } 
        catch(Exception e)
        {
            System.out.println("Error in loading in main()!!! "+e.toString());
        }
        // finally {
        //     // This ONE LINE ensures pools are closed
        //     DataSourceSingleton.shutdownAll();
        // }
    }

    public void callableMain(String mode) {
        try {
            
            long startTime = System.currentTimeMillis();

            Dao d=new Dao(mode);

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

            // (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"),17, "all");
            // (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"), 12, "all");
            // (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"),9, "all");
            // (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"),10, "all");
            // (new OnuSerialFillerApp()).fillOnuSerial(new Dao("prod"),18, "all");

            long endTime = System.currentTimeMillis();
            double durationInSeconds = (endTime - startTime) / 1000.0;
            System.out.println("Total duration time taken: " + durationInSeconds + "seconds");
        } 
        catch(Exception e)
        {
            System.out.println("Error in loading in main()!!! "+e.toString());
        }
        // finally {
        //     // This ONE LINE ensures pools are closed
        //     DataSourceSingleton.shutdownAll();
        // }
    }

}