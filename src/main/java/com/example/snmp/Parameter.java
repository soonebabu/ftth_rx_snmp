package com.example.snmp;

public class Parameter {
    private int threadPool = 100;      // default
    private int threadPerNode = 5;     // default
    private int batchSize = 5;         // default

    
    public Parameter() {
    }

    public int getThreadPool() {
        return threadPool;
    }
    public void setThreadPool(int threadPool) {
        this.threadPool = threadPool;
    }
    public int getThreadPerNode() {
        return threadPerNode;
    }
    public void setThreadPerNode(int threadPerNode) {
        this.threadPerNode = threadPerNode;
    }
    public int getBatchSize() {
        return batchSize;
    }
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    

    

}
