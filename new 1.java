package com.example.snmp;

import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.example.snmp.NodeType;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class App {

    public static void main(String[] args) {

        // time
        long programStart = System.currentTimeMillis();
        Dao dao = new Dao();
        List<NodeType> nodeTypes = dao.getNodeTypes("ftth");
        System.out.println(nodeTypes.size());
        List<Node> nodes = dao.getNodes(17);
        System.out.println(nodes.get(1)); // Use get() instead of array syntax

        String[] oltIps = nodes.stream()
                .map(Node::getIp) // extract IP from Node
                .limit(252) // take first 10
                .toArray(String[]::new); // collect as String[]

        // String[] oltIps = nodes.stream()
        // .map(Node::getIp) // extract IP from each Node
        // .toArray(String[]::new); // convert to array

        System.out.println("Total Nodes: " + oltIps.length);

        // String[] oltIps = {
        // "10.28.154.218", // OLT 1 charikot
        // "10.28.151.158", // OLT 2 jiri
        // "10.28.155.75", // manthali2
        // "10.28.151.30", // manthali1
        // "10.28.159.34" // salyan
        // };

        String community = "public";
        String baseOid = "1.3.6.1.4.1.3902.1082.500.20.2.2.2.1.10.285278465"; // onu rx level
        // String baseOid = "1.3.6.1.4.1.3902.1012.3.28.1.1.2.268501248"; // for
        // description

        ExecutorService executor = Executors.newFixedThreadPool(oltIps.length);
        // ExecutorService executor = Executors.newFixedThreadPool(100);

        // Thread-safe list to collect results from all threads
        List<String> resultList = Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = new ArrayList<>();

        for (String ip : oltIps) {
            Future<?> future = executor.submit(() -> {
                try {
                    sendSnmpGet(ip, community, baseOid, resultList);
                } catch (IOException e) {
                    resultList.add("❌ Error querying " + ip + ": " + e.getMessage());
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

        // Print combined results
        System.out.println("\n======= Combined SNMP Results =======");
        for (String res : resultList) {
            System.out.println(res);
        }

        long programEnd = System.currentTimeMillis();
        System.out.println("\n✅ Total time for all OLTs from main: " + (programEnd - programStart) + " ms");

    }

    private static void sendSnmpGet(String ipAddress, String community, String baseOid, List<String> resultList)
            throws IOException {
        List<String> oidList = new ArrayList<>();

        // First check if IP is reachable (ping)
        boolean reachable = InetAddress.getByName(ipAddress).isReachable(2000); // 2 seconds timeout

        if (!reachable) {
            resultList.add("❌ " + ipAddress + " is not reachable (ping failed)");
            return;
        }

        for (int i = 1; i <= 10; i++) {
            oidList.add(baseOid + "." + i + "." + 1); // for onu rx level
            // oidList.add(baseOid + "." + i ); // for description

        }

        long start = System.currentTimeMillis();

        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        transport.listen();
        CommunityTarget target= getCommunityTarget(community,ipAddress);
        

        PDU pdu = new PDU();
        for (String oid : oidList) {
            pdu.add(new VariableBinding(new OID(oid)));
        }
        pdu.setType(PDU.GET);

        Snmp snmp = new Snmp(transport);

        ResponseEvent response = snmp.get(pdu, target);

        if (response != null && response.getResponse() != null) {
            PDU responsePDU = response.getResponse();
            resultList.add("\n✅ Response from " + ipAddress + ":");

            for (VariableBinding vb : responsePDU.getVariableBindings()) {
                Variable var = vb.getVariable();
                float ontRxpowerNew;

                try {
                    String strValue = var.toString();
                    float value = Float.parseFloat(strValue);
                    ontRxpowerNew = (value == 65535f) ? 0f : (value * 0.002f - 30f);
                } catch (NumberFormatException e) {
                    ontRxpowerNew = 0f;
                }

                resultList.add(vb.getOid() + " = " + String.format("%.2f", ontRxpowerNew));
            }
        } else {
            resultList.add("⚠ No response from " + ipAddress);
        }

        snmp.close();

        long end = System.currentTimeMillis();
        double totalTimeInSeconds = (end - start) / 1000.0;

        resultList.add("⏱ Time for " + ipAddress + ": " + String.format("%.3f", totalTimeInSeconds) + " seconds");

    }

    public static CommunityTarget getCommunityTarget(String community,String ipAddress)
    {
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setVersion(SnmpConstants.version2c);
        target.setAddress(new UdpAddress(ipAddress + "/161"));
        target.setRetries(2);
        target.setTimeout(2500);
        return target;
    }
}
