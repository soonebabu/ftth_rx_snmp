package com.example.snmp;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.sql.Timestamp;

//for logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;

public class HelperClass {

    // Global instance
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public boolean isReachable(Node node, Dao dao) {
        boolean reachable = false;
        String ip = node.getIp();
        try {
            reachable = InetAddress.getByName(ip).isReachable(2000);
        } catch (Exception e) {
            dao.updateReachable(node);
            logger.error("Error:: " + ip + ":::" + e.toString());
        }
        return reachable;
    }

    public void logInfo(String msg) {
        System.out.println("ℹ️ " + msg);
    }

    public CommunityTarget getCommunityTarget(Node node) {
        CommunityTarget target = new CommunityTarget();
        String ipAddress = node.getIp();
        String community = node.getSnmpcommunity();
        target.setCommunity(new OctetString(community));
        target.setVersion(SnmpConstants.version2c);
        target.setAddress(new UdpAddress(ipAddress + "/161"));
        target.setRetries(1);
        target.setTimeout(10000);
        return target;
    }

    public String macToCustomFormat(String mac) {
    if (mac == null) return null;

    String[] parts = mac.split(":");
    StringBuilder sb = new StringBuilder();

    // First 4 bytes → ASCII
    for (int i = 0; i < 4 && i < parts.length; i++) {
        int value = Integer.parseInt(parts[i], 16);
        sb.append((char) value);  // direct ASCII
    }

    // Remaining bytes → HEX (uppercase, no ":")
    for (int i = 4; i < parts.length; i++) {
        sb.append(parts[i].toUpperCase());
    }

    return sb.toString().toUpperCase(); // whole string uppercase
}


public Timestamp parseSnmpDate(String input) {
    try {
        // Case 1: Already formatted string (yyyy-MM-dd HH:mm:ss)
        if (input.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            LocalDateTime ldt = LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return Timestamp.valueOf(ldt);
        }

        // Case 2: Hex string (SNMP format)
        String[] parts = input.split(":");
        if (parts.length < 7) {
            return null; // invalid
        }

        int year   = Integer.parseInt(parts[0] + parts[1], 16);
        int month  = Integer.parseInt(parts[2], 16);
        int day    = Integer.parseInt(parts[3], 16);
        int hour   = Integer.parseInt(parts[4], 16);
        int minute = Integer.parseInt(parts[5], 16);
        int second = Integer.parseInt(parts[6], 16);

        if (month < 1 || month > 12 ||
            day < 1 || day > 31 ||
            hour < 0 || hour > 23 ||
            minute < 0 || minute > 59 ||
            second < 0 || second > 60) {
            return null; // invalid
        }

        LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, minute, second);

        // Case 2a: Huawei format with timezone info
        if (parts.length >= 11) {
            String direction = new String(hexToBytes(parts[8])); // '+' or '-'
            int tzHour = Integer.parseInt(parts[9], 16);
            int tzMin  = Integer.parseInt(parts[10], 16);

            int offsetMinutes = tzHour * 60 + tzMin;
            if (direction.equals("-")) offsetMinutes = -offsetMinutes;

            OffsetDateTime odt = ldt.atOffset(ZoneOffset.ofTotalSeconds(offsetMinutes * 60));
            return Timestamp.from(odt.toInstant());
        }

        // Case 2b: ZTE format (no TZ, assume system default zone)
        return Timestamp.valueOf(ldt);

    } catch (Exception e) {
        return null; // fallback
    }
}

// helper: hex -> byte[]
private static byte[] hexToBytes(String hex) {
    int len = hex.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                             + Character.digit(hex.charAt(i+1), 16));
    }
    return data;
}
    

}
