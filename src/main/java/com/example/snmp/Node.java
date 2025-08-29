package com.example.snmp;

import java.sql.Timestamp;

public class Node {
    private int id;
    private String name;
    private int type;
    private String ip;
    private String assignedID;
    private String shelfncard;
    private String service;
    private int portspercard;
    private String region;
    private String exchange;
    private String snmpcommunity;
    private String snmpwritecommunity;
    private String sysname;
    private Timestamp timestamp;

    // Constructors
    public Node() {
    }

    public Node(int id, String name, int type, String ip, String assignedID, String shelfncard,
            String service, int portspercard, String region, String exchange,
            String simpcommunity, String snmpwritecommunity, String sysname,
            Timestamp timestamp) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.ip = ip;
        this.assignedID = assignedID;
        this.shelfncard = shelfncard;
        this.service = service;
        this.portspercard = portspercard;
        this.region = region;
        this.exchange = exchange;
        this.snmpcommunity = simpcommunity;
        this.snmpwritecommunity = snmpwritecommunity;
        this.sysname = sysname;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAssignedID() {
        return assignedID;
    }

    public void setAssignedID(String assignedID) {
        this.assignedID = assignedID;
    }

    public String getShelfncard() {
        return shelfncard;
    }

    public void setShelfncard(String shelfncard) {
        this.shelfncard = shelfncard;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public int getPortspercard() {
        return portspercard;
    }

    public void setPortspercard(int portspercard) {
        this.portspercard = portspercard;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getSnmpcommunity() {
        return snmpcommunity;
    }

    public void setSnmpcommunity(String simpcommunity) {
        this.snmpcommunity = simpcommunity;
    }

    public String getSnmpwritecommunity() {
        return snmpwritecommunity;
    }

    public void setSnmpwritecommunity(String snmpwritecommunity) {
        this.snmpwritecommunity = snmpwritecommunity;
    }

    public String getSysname() {
        return sysname;
    }

    public void setSysname(String sysname) {
        this.sysname = sysname;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    // toString method for debugging/logging purposes
    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", ip='" + ip + '\'' +
                ", assignedID='" + assignedID + '\'' +
                ", shelfncard='" + shelfncard + '\'' +
                ", service='" + service + '\'' +
                ", portspercard=" + portspercard +
                ", region='" + region + '\'' +
                ", exchange='" + exchange + '\'' +
                ", simpcommunity='" + snmpcommunity + '\'' +
                ", snmpwritecommunity='" + snmpwritecommunity + '\'' +
                ", sysname='" + sysname + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}