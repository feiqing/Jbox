package com.github.jbox.oplog;

import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/11/12 5:27 PM.
 */
@Data
public class OplogConfig {

    // 指定需要同步的MongoDB[必填]
    private List<Mongo> mongos;

    // 每次读取oplog大小
    private int logBatchSize = 1024 * 200;

    // 指定需要哪些操作[必填]
    private Set<String> ops;

    public void setOps(Set<Op> ops) {
        this.ops = ops.stream().map(Op::getOp).collect(toSet());
    }

    // 指定需要同步的表[非必填]
    private List<Predicate<String>> tables;

    public void setTables(List<String> tables) {
        this.tables = tables.stream().map(table -> Pattern.compile(table).asPredicate()).collect(toList());
    }

    // 如何消费[必填]
    private OplogHandler handler;

    // disruptor 相关
    private int ringBufferConcurrency = 64;

    private int ringBufferSize = 1024 * 512;

    public enum Op {
        insert("i"),
        update("u"),
        delete("d"),
        cmd("c"),
        db("db"),
        noop("n");

        Op(String op) {
            this.op = op;
        }

        @Getter
        String op;
    }
}
