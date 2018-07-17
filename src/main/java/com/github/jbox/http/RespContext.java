package com.github.jbox.http;

import lombok.Data;

import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-07 19:56:00.
 */
@Data
public class RespContext {

    private String resp;

    private AbstractHttpClient client;

    private String path;

    private Map<String, String> params;
}
