package com.example.snmp;

public class NodeSerialOid {
    private int id;
    private String ip;
    private String name;
    private int serialId;
    private int onuid;
    private String serial;
    private String oidDesc;
    private int nodeType;
    private String oidStatus;
    private String oidOnuRxPower;
    private String oidSerial;
    private String oidOltRxPower;
    private String oidTemperature;
    private String oidLineProfile;
    private String oidDistance;
    private float onuRxPower;
    private float onuOltRxPower;
    private float onuDistance;
    private float onuTemperature;
    



    

     public float getOnuTemperature() {
        return onuTemperature;
    }


    public void setOnuTemperature(float onuTemperature) {
        this.onuTemperature = onuTemperature;
    }


    // Constructors
    public NodeSerialOid() {
    }  


    public float getOnuRxPower() {
        return onuRxPower;
    }



    public void setOnuRxPower(float onuRxPower) {
        this.onuRxPower = onuRxPower;
    }



    public float getOnuOltRxPower() {
        return onuOltRxPower;
    }



    public void setOnuOltRxPower(float onuOltRxPower) {
        this.onuOltRxPower = onuOltRxPower;
    }



    public float getOnuDistance() {
        return onuDistance;
    }



    public void setOnuDistance(float onuDistance) {
        this.onuDistance = onuDistance;
    }



   
    

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSerialId() {
        return serialId;
    }

    public void setSerialId(int serialId) {
        this.serialId = serialId;
    }

    public int getOnuid() {
        return onuid;
    }

    public void setOnuid(int omid) {
        this.onuid = omid;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getOidDesc() {
        return oidDesc;
    }

    public void setOidDesc(String oidDesc) {
        this.oidDesc = oidDesc;
    }

    public int getNodeType() {
        return nodeType;
    }

    public void setNodeType(int nodeType) {
        this.nodeType = nodeType;
    }

    public String getOidStatus() {
        return oidStatus;
    }

    public void setOidStatus(String oidStatus) {
        this.oidStatus = oidStatus;
    }

    public String getOidOnuRxPower() {
        return oidOnuRxPower;
    }

    public void setOidOnuRxPower(String oidOmuxPower) {
        this.oidOnuRxPower = oidOmuxPower;
    }

    public String getOidSerial() {
        return oidSerial;
    }

    public void setOidSerial(String oidSerial) {
        this.oidSerial = oidSerial;
    }

    public String getOidOltRxPower() {
        return oidOltRxPower;
    }

    public void setOidOltRxPower(String oidOluxPower) {
        this.oidOltRxPower = oidOluxPower;
    }

    public String getOidTemperature() {
        return oidTemperature;
    }

    public void setOidTemperature(String oidTemperature) {
        this.oidTemperature = oidTemperature;
    }

    public String getOidLineProfile() {
        return oidLineProfile;
    }

    public void setOidLineProfile(String oidLineProfile) {
        this.oidLineProfile = oidLineProfile;
    }

    public String getOidDistance() {
        return oidDistance;
    }

    public void setOidDistance(String oidDistance) {
        this.oidDistance = oidDistance;
    }    


  


    @Override
    public String toString() {
        return "NodeSerialOid [id=" + id + ", ip=" + ip + ", name=" + name + ", serialId=" + serialId + ", onuid="
                + onuid + ", serial=" + serial + ", oidDesc=" + oidDesc + ", nodeType=" + nodeType + ", oidStatus="
                + oidStatus + ", oidOnuRxPower=" + oidOnuRxPower + ", oidSerial=" + oidSerial + ", oidOltRxPower="
                + oidOltRxPower + ", oidTemperature=" + oidTemperature + ", oidLineProfile=" + oidLineProfile
                + ", oidDistance=" + oidDistance + ", onuRxPower=" + onuRxPower + ", onuOltRxPower=" + onuOltRxPower
                + ", onuDistance=" + onuDistance + ", onuTemperature=" + onuTemperature + ", community=" ;
    }

    
    

    
}