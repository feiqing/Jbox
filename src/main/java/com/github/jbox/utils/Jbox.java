package com.github.jbox.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Jbox提供的一些工具方法
 *
 * @author jifang@alibaba-inc.com
 * @since 16/8/18 下午6:09.
 */
@Slf4j
public class Jbox {

    private static String ip = "unknown";

    private static String mac = "unknown";

    static {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                String eth = networkInterface.getName();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address && StringUtils.equalsAnyIgnoreCase(eth, "eth0"/*Linux*/, "en0"/*Mac*/)) {
                        ip = inetAddress.getHostAddress();
                        break;
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

    public static String getLocalIp() {
        return ip;
    }

    public static String getLocalMac() {
        return mac;
    }

    public static Method getAbstractMethod(JoinPoint pjp) {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        return ms.getMethod();
    }

    public static Method getImplMethod(JoinPoint pjp) throws NoSuchMethodException {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            method = pjp.getTarget().getClass().getDeclaredMethod(ms.getName(), method.getParameterTypes());
        }
        return method;
    }

    private static final ConcurrentMap<Class<?>, Class<?>> primitives = new ConcurrentHashMap<>();

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
