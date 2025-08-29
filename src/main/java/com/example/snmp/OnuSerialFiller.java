package com.example.snmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//for logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnuSerialFiller {

    // Global instance
    private final Logger logger = LoggerFactory.getLogger(App.class);
    private HelperClass hc = new HelperClass();

    public void fillOnuSerial(Dao dao, int nType, String region) {
        logger.info("From OnuSerialFiller");
        System.out.println("From OnuSerialFiller");
        ExecutorService executor = Executors.newFixedThreadPool(100);

        List<Future<?>> futures = new ArrayList<>();

        NodeType nodeType = dao.getNodeTypeById(nType);

        List<Node> nodes = dao.getNodes(nType, region);
        logger.info("Total Nodes = " + nodes.size());

        for (Node node : nodes) {

            if (!hc.isReachable(node, dao)) {
                logger.error(node.getIp() + " is not reachable (ping failed)");
                continue;
            }

            Future<?> future = executor.submit(() -> {
                try {

                    sendSnmpWalkAllOnus(node, nodeType, dao);

                } catch (Exception e) {
                    System.out.println("❌ Error querying " + node.getIp() + ": " + e.getMessage());
                    logger.error("❌ Error querying " + node.getIp() + ": " + e.getMessage());
                }
            });
            futures.add(future);

        }

    }

    private void sendSnmpWalkAllOnus(Node node, NodeType nodeType, Dao dao) {

    }
}