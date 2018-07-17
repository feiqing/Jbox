package com.github.jbox.hbase;

import lombok.Data;

import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-06-27 10:26:00.
 */
@Data
@Table("t_forward")
public class TForward implements HBaseMode {

    @Qualifier(exclude = true)
    private Long id;

    private String mac;

    private String ip;

    @Qualifier(exclude = true)
    private Integer port;

    private String extend;

    @Qualifier(value = "extendMap")
    private Map<String, String> extendMap;

    public static TForward newForward(String mac, String ip, int port) {
        TForward forward = new TForward();
        forward.setMac(mac);
        forward.setIp(ip);
        forward.setPort(port);

        return forward;
    }

    @Override
    public String getRowKey() {
        return String.valueOf(id);
    }

    @Override
    public void setRowKey(String rowKey) {
        this.setId(Long.valueOf(rowKey));
    }
}
