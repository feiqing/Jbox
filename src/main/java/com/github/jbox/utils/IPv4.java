package com.github.jbox.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-04 19:26:00.
 */
@Slf4j
public class IPv4 {

    private static volatile ConcurrentMap<String, List<String>> ethName2ips = null;

    public static final List<String> ethNames = new ArrayList<>();

    static {
        ethNames.add("eth0");
        ethNames.add("en0");
    }

    public static String getLocalIp() {
        ConcurrentMap<String, List<String>> localIps = getLocalIps();
        for (String ethName : ethNames) {
            List<String> ips = localIps.get(ethName);
            if (ips != null) {
                return ips.get(0);
            }
        }

        return "127.0.0.1";
    }

    public static String getLocalIp(String ethName) {
        return Optional.ofNullable(getLocalIps().get(ethName)).map(ips -> ips.get(0)).orElse(null);
    }

    public static ConcurrentMap<String, List<String>> getLocalIps() {
        return ethName2ips == null ? (ethName2ips = doGetLocalIps()) : ethName2ips;
    }

    private static ConcurrentMap<String, List<String>> doGetLocalIps() {
        ConcurrentMap<String, List<String>> ethName2ips = new ConcurrentHashMap<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                String ethName = networkInterface.getName();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address) {
                        ethName2ips.computeIfAbsent(ethName, _k -> new ArrayList<>(1)).add(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (Throwable e) {
            log.error("", e);
        }

        return ethName2ips;
    }

    public static void main(String[] args) {
        System.out.println(IPv4.getLocalIp());
    }
}
