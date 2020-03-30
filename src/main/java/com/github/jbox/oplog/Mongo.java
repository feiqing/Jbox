package com.github.jbox.oplog;

import lombok.Data;

import java.io.Serializable;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/24 9:29 PM.
 */
@Data
public class Mongo implements Serializable {

    private static final long serialVersionUID = -2081965221916801594L;

    private String url;

    private String db;

    public static Mongo newInstance(String url, String db) {
        Mongo mongo = new Mongo();
        mongo.url = url;
        mongo.db = db;
        return mongo;
    }
}
