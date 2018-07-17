package com.github.jbox.websocket;

import lombok.Data;

import java.util.Objects;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-13 18:55:00.
 */
@Data
public class SocketKey {

    private String group;

    private String topic;

    public SocketKey() {
    }

    public SocketKey(String group, Object topic) {
        this.group = group;
        this.topic = String.valueOf(topic);
    }

    public SocketKey(String group, String topic) {
        this.group = group;
        this.topic = topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        SocketKey that = (SocketKey) o;
        return Objects.equals(getGroup(), that.getGroup()) &&
                Objects.equals(getTopic(), that.getTopic());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGroup(), getTopic());
    }

    @Override
    public String toString() {
        return String.format("/%s/%s", group, topic);
    }
}
