package com.example.snmp;

import java.sql.Timestamp;

public class OnuSerial {
    private int id;
    private int nodeid;
    private int onuid;
    private String serial;
    private String name;
    private Float onuRxPower; // Assuming these are floating point numbers
    private Float oltRxPower; // as their types weren't specified
    private Float distance; // in the original structure
    private Float temperature; // same assumption
    private Timestamp timestamp;
    private Timestamp onuLastOnline;

    public OnuSerial() {
    }

    public int getId() {
        return id;
    }

    public int getNodeid() {
        return nodeid;
    }

    public int getOnuid() {
        return onuid;
    }

    public String getSerial() {
        return serial;
    }

    public String getName() {
        return name;
    }

    public Float getOnuRxPower() {
        return onuRxPower;
    }

    public Float getOltRxPower() {
        return oltRxPower;
    }

    public Float getDistance() {
        return distance;
    }

    public Float getTemperature() {
        return temperature;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNodeid(int nodeid) {
        this.nodeid = nodeid;
    }

    public void setOnuid(int onuid) {
        this.onuid = onuid;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOnuRxPower(Float onuRxPower) {
        this.onuRxPower = onuRxPower;
    }

    public void setOltRxPower(Float oltRxPower) {
        this.oltRxPower = oltRxPower;
    }

    public void setDistance(Float distance) {
        this.distance = distance;
    }

    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "OnuSerial [id=" + id + ", nodeid=" + nodeid + ", onuid=" + onuid + ", serial=" + serial + ", name="
                + name + ", onuRxPower=" + onuRxPower + ", oltRxPower=" + oltRxPower + ", distance=" + distance
                + ", temperature=" + temperature + ", timestamp=" + timestamp + "]";
    }

    public Timestamp getOnuLastOnline() {
        return onuLastOnline;
    }

    public void setOnuLastOnline(Timestamp onuLastOnline) {
        this.onuLastOnline = onuLastOnline;
    }

}