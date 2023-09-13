package com.alibaba.jbox.domain;

import java.util.HashMap;
import java.util.Map;

import lombok.ToString;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * @author jifang
 * @since 2017/3/1 下午2:40.
 */
@ToString
public class Address {
    private String local;

    private double lat;

    private double lng;

    public Address() {
    }

    public Address(String local, double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
        this.local = local;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public static void main(String[] args) {
        Map<String, Object> params = new HashMap<>();
        params.put("local", "test");
        params.put("lat", "34.1x");
        Address address = new Address();
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(address);
        wrapper.setAutoGrowNestedPaths(true);
        MutablePropertyValues newPvs = new MutablePropertyValues(params);
        wrapper.setPropertyValues(newPvs, true);
        System.out.println(address);
    }
}
