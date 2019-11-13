package com.github.jbox.oplog;

import lombok.Data;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.io.Serializable;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/22 9:09 PM.
 */
@Data
public class Oplog implements Serializable {

    private static final long serialVersionUID = 5149553535664069336L;

    // mongo 配置相关
    private String url;

    private String db;

    private String ns;

    private String op;

    // update存在
    private Document where;

    private Document data;

    private BsonTimestamp ts;
}
