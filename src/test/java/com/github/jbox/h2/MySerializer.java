package com.github.jbox.h2;

import com.github.jbox.serializer.AbstractSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/24 7:02 PM.
 */
public class MySerializer extends AbstractSerializer {

    @Override
    protected byte[] doSerialize(Object obj) throws Throwable {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS").format((LocalDateTime) obj).getBytes();
    }

    @Override
    protected Object doDeserialize(byte[] bytes) throws Throwable {
        TemporalAccessor parse = LocalDateTime.parse(new String(bytes), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS"));
        return parse;
    }
}
