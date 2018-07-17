package com.github.jbox.utils;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-04 19:29:00.
 */
@Slf4j
public class Mac {

    private static final String DEFAULT_MAC_ADDR = "00:00:00:00:00:00";

    private static List<String> macAddrs = null;

    private static String preferAddr = null;

    public static String getLocalMacAddr() {
        if (!Strings.isNullOrEmpty(preferAddr)) {
            return preferAddr;
        }

        return getLocalMacAddrs().get(0);
    }

    public static String getLocalMacAddr(String prefer) {
        if (!Strings.isNullOrEmpty(preferAddr)) {
            return preferAddr;
        }

        for (String localMacAddr : getLocalMacAddrs()) {
            if (StringUtils.contains(localMacAddr, prefer)) {
                return preferAddr = localMacAddr;
            }
        }

        return getLocalMacAddrs().get(0);
    }

    public static List<String> getLocalMacAddrs() {
        return macAddrs == null ? (macAddrs = doGetLocalMacAddrs()) : macAddrs;
    }

    private static List<String> doGetLocalMacAddrs() {
        List<String> macAddrs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();

            while (netInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = netInterfaces.nextElement();

                byte[] hardwareAddress = networkInterface.getHardwareAddress();
                if (hardwareAddress == null) {
                    continue;
                }

                if (!isValidIp(networkInterface)) {
                    continue;
                }

                macAddrs.add(getMacFromBytes(hardwareAddress));
            }
        } catch (Throwable e) {
            log.error("", e);
        }

        if (macAddrs.isEmpty()) {
            macAddrs.add(DEFAULT_MAC_ADDR);
        }

        return macAddrs;
    }

    private static String getMacFromBytes(byte[] macBytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < macBytes.length; i++) {
            if (i != 0) {
                sb.append(":");
            }

            String str = Integer.toHexString(macBytes[i] & 0xff);
            if (str.length() == 1) {
                sb.append("0");
            }

            sb.append(str);
        }

        return sb.toString();
    }

    private static boolean isValidIp(NetworkInterface networkInterface) {
        Enumeration<InetAddress> ips = networkInterface.getInetAddresses();
        while (ips.hasMoreElements()) {
            InetAddress ip = ips.nextElement();
            if (/*ip instanceof Inet4Address && */!ip.isLoopbackAddress()) {
                return true;
            }
        }

        return false;
    }
}
