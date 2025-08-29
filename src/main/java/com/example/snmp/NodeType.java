package com.example.snmp;

import java.security.Timestamp;

public class NodeType {
    private int id;
    private String name;
    private String vendor;
    private String oidOnuSerial;
    private String oidOnuDescription;
    private String oidOnuLastOnDateTime;
    private String oidOnuDistance;
    private String oidOnuRxPower;


    public NodeType() {
    }

    

    

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

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getOidOnuSerial() {
        return oidOnuSerial;
    }

    public void setOidOnuSerial(String oidOnuSerial) {
        this.oidOnuSerial = oidOnuSerial;
    }

    public String getOidOnuDescription() {
        return oidOnuDescription;
    }

    public void setOidOnuDescription(String oidOnuDescription) {
        this.oidOnuDescription = oidOnuDescription;
    }

    public String getOidOnuLastOnDateTime() {
        return oidOnuLastOnDateTime;
    }

    public void setOidOnuLastOnDateTime(String oidOnuLastOnDateTime) {
        this.oidOnuLastOnDateTime = oidOnuLastOnDateTime;
    }



    public String getOidOnuDistance() {
        return oidOnuDistance;
    }



    public void setOidOnuDistance(String oidOnuDistance) {
        this.oidOnuDistance = oidOnuDistance;
    }





    public String getOidOnuRxPower() {
        return oidOnuRxPower;
    }





    public void setOidOnuRxPower(String oidOnuRxPower) {
        this.oidOnuRxPower = oidOnuRxPower;
    }

}