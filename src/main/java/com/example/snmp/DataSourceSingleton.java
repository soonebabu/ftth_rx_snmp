package com.example.snmp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataSourceSingleton {
    private static final Map<String, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>();

    private DataSourceSingleton() {
        // prevent instantiation
    }

    // Add this simple shutdown method
    public static void shutdownAll() {
         for (Map.Entry<String, HikariDataSource> entry : dataSourceMap.entrySet()) {
        String mode = entry.getKey();
        HikariDataSource ds = entry.getValue();
        if (ds != null && !ds.isClosed()) {
            System.out.println("Closing DB pool for mode: " + mode);
            ds.close();
        }
    }
    dataSourceMap.clear();
    }

    // Add this static block to your class
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownAll();
        }));
    }

    public static DataSource getDataSource(String mode) {
        return dataSourceMap.computeIfAbsent(mode, DataSourceSingleton::createDataSource);
    }

    private static HikariDataSource createDataSource(String mode) {
        HikariConfig config = new HikariConfig();

        switch (mode) {
            case "prod1":// test db
                config.setJdbcUrl("jdbc:mysql://localhost:3306/adslprofile_20250821");
                // config.setJdbcUrl("jdbc:mysql://172.17.0.1:3306/adslprofile_202504");

                config.setUsername("phpmyadmin");
                config.setPassword("ntc");

                break;

            case "prod":
                config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/adslprofile");
                // config.setJdbcUrl("jdbc:mysql://172.17.0.1:3306/adslprofile_202504");

                config.setUsername("phpmyadmin");
                config.setPassword("ntc");

                break;

            default:
                config.setJdbcUrl("jdbc:mysql://localhost:3310/adslprofile"); // test db at localcomputer
                config.setUsername("root");
                config.setPassword("root");
                break;
        }

        config.setMaximumPoolSize(100);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(20000);

        HikariDataSource ds = new HikariDataSource(config);

        // // shutdown hook for each pool
        // Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        //     System.out.println("Shutting down DB pool for mode: " + mode);
        //     ds.close();
        // }));

        return ds;
    }
}
