package com.github.jbox.oplog;

import lombok.Data;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/24 9:29 PM.
 */
@Data
public class Mongo {

    private String url;

    private String db;

    public static Mongo newInstance(String url, String db) {
        Mongo mongo = new Mongo();
        mongo.url = url;
        mongo.db = db;
        return mongo;
    }
}
