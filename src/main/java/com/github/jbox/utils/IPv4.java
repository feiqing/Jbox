package com.github.jbox.utils;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-04 19:26:00.
 */
@Slf4j
public class IPv4 {

    private static final String DEFAULT_IP = "127.0.0.1";

    private static String preferLocalIP = null;

    private static List<String> localIps = null;

    public static String getLocalIp() {
        if (!Strings.isNullOrEmpty(preferLocalIP)) {
            return preferLocalIP;
        }

        return getLocalIps().get(0);
    }

    public static String getLocalIp(String prefer) {
        if (!Strings.isNullOrEmpty(preferLocalIP)) {
            return preferLocalIP;
        }

        for (String localIP : getLocalIps()) {
            if (StringUtils.contains(localIP, prefer)) {
                return preferLocalIP = localIP;
            }
        }

        return getLocalIps().get(0);
    }

    public static List<String> getLocalIps() {
        return localIps == null ? (localIps = doGetLocalIps()) : localIps;
    }

    private static List<String> doGetLocalIps() {
        List<String> localIps = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        localIps.add(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (Throwable e) {
            log.error("", e);
        }

        if (localIps.isEmpty()) {
            localIps.add(DEFAULT_IP);
        }

        return localIps;
    }
}
