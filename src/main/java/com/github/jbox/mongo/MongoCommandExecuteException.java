package com.github.jbox.mongo;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-06 15:19:00.
 */
public class MongoCommandExecuteException extends RuntimeException {

    private static final long serialVersionUID = -8181715563606319069L;

    @Getter
    @Setter
    private int errorCode;

    @Getter
    @Setter
    private String codeName;

    @Getter
    @Setter
    private int ok;

    @Getter
    @Setter
    private String errmsg;

    @Override
    public String toString() {
        return "MongoCommandExecuteException{" +
                "errorCode=" + errorCode +
                ", codeName='" + codeName + '\'' +
                ", ok=" + ok +
                ", errmsg='" + errmsg + '\'' +
                '}';
    }
}
