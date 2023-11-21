package com.alibaba.jbox.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Jbox提供的一些工具方法
 *
 * @author jifang@alibaba-inc.com
 * @since 16/8/18 下午6:09.
 */
@Slf4j
public class Jbox {

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    private static final ConcurrentMap<Class<?>, Class<?>> primitives = new ConcurrentHashMap<>();

    private static String ip = "unknown";

    private static String mac = "unknown";

    static {
        primitives.put(byte.class, Byte.class);
        primitives.put(Byte.class, Byte.class);
        primitives.put(short.class, Short.class);
        primitives.put(Short.class, Short.class);
        primitives.put(int.class, Integer.class);
        primitives.put(Integer.class, Integer.class);
        primitives.put(long.class, Long.class);
        primitives.put(Long.class, Long.class);
        primitives.put(float.class, Float.class);
        primitives.put(Float.class, Float.class);
        primitives.put(double.class, Double.class);
        primitives.put(Double.class, Double.class);
        primitives.put(boolean.class, Boolean.class);
        primitives.put(Boolean.class, Boolean.class);
    }

    static {
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                ip = localAddress.getHostAddress();
            } else {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                if (interfaces != null) {
                    while (interfaces.hasMoreElements()) {
                        try {
                            NetworkInterface network = interfaces.nextElement();
                            Enumeration<InetAddress> addresses = network.getInetAddresses();
                            while (addresses.hasMoreElements()) {
                                try {
                                    InetAddress address = addresses.nextElement();
                                    if (isValidAddress(address)) {
                                        ip = address.getHostAddress();
                                    }
                                } catch (Throwable e) {
                                    log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                                }
                            }
                        } catch (Throwable e) {
                            log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log.error("get local ip addr error.", t);
        }


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

                mac = getMacFromBytes(hardwareAddress);
            }
        } catch (Throwable e) {
            log.error("get local mac addr error.", e);
        }
    }

    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }

        String name = address.getHostAddress();
        return (name != null && !"0.0.0.0".equals(name) && !"127.0.0.1".equals(name) && IP_PATTERN.matcher(name).matches());
    }

    public static String getLocalIp() {
        return ip;
    }

    public static String getLocalMac() {
        return mac;
    }

    public static <T> T toObj(String val, Class<T> type) {
        Object instance;
        Class<?> primitiveType = primitives.get(type);
        if (primitiveType != null) {
            try {
                instance = primitiveType.getMethod("valueOf", String.class).invoke(null, val);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        } else if (type == Character.class || type == char.class) {
            instance = val.charAt(0);
        } else if (type == String.class) {
            instance = val;
        } else {
            instance = JSON.parseObject(val, type);
        }

        return (T) instance;
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

    public static void main(String[] args) {
        System.out.println(getLocalIp());
        System.out.println(getLocalMac());
    }
}
