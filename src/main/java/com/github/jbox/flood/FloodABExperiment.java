package com.github.jbox.flood;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * flood AB测试框架
 *
 * @author jifang
 * @since 2016/11/3 下午2:36.
 */
public abstract class FloodABExperiment {

    protected static final Logger FLOOD_LOGGER = LoggerFactory.getLogger(FloodABExperiment.class);

    @SuppressWarnings("unchecked")
    public <T> T doExperiment(Object param) {
        int hash = Math.abs(Objects.hashCode(param));
        double curRate = (hash % 100) * 1.0;
        if (curRate < getRate()) {
            FLOOD_LOGGER.debug("via inValue {}", getInValue());
            return (T) getInValue();
        } else {
            FLOOD_LOGGER.debug("via outValue {}", getOutValue());
            return (T) getOutValue();
        }
    }

    protected abstract double getRate();

    protected abstract Object getInValue();

    protected abstract Object getOutValue();
}
