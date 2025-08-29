package com.example.snmp;

public class OnuSerialWithOidClass {
    public OnuSerial onuSerial = null;
    public String oidOnuSerial = null;

    public OnuSerialWithOidClass() {
    }

    public OnuSerial getOnuSerial() {
        return onuSerial;
    }

    public void setOnuSerial(OnuSerial onuSerial) {
        this.onuSerial = onuSerial;
    }

    public String getOidOnuSerial() {
        return oidOnuSerial;
    }

    public void setOidOnuSerial(String oidOnuSerial) {
        this.oidOnuSerial = oidOnuSerial;
    }

    @Override
    public String toString() {
        return "OnuSerialWithOidClass [onuSerial=" + onuSerial + ", oidOnuSerial=" + oidOnuSerial + ", getOnuSerial()="
                + getOnuSerial() + ", getOidOnuSerial()=" + getOidOnuSerial() + ", getClass()=" + getClass()
                + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
    }

}
