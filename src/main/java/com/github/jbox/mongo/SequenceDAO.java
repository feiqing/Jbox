package com.github.jbox.mongo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.github.jbox.mongo.TableConstant.*;

/**
 * 每次启动, Sequence抬升1000
 *
 * @author cunxiao
 * @since 2018-02-28 17:03:00.
 **/
public class SequenceDAO {

    @Setter
    @Getter
    private int sequenceStep = 1000;

    private MongoOperations mongoTemplate;

    private ConcurrentMap<String, Integer> nextIncStep = new ConcurrentHashMap<>();

    public SequenceDAO(MongoOperations mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 生成一个自增ID(有副作用)
     *
     * @param type: 表名
     */
    public long generateId(String type) {
        return generateSequence(type).getSeq();
    }

    /**
     * 查找最新的自增ID(无副作用)
     *
     * @param type: 表名
     */
    public long getSequence(String type) {
        Query where = new Query(Criteria.where(ID).is(type));
        return mongoTemplate.findOne(where, Sequence.class).getSeq();
    }

    private Sequence generateSequence(String type) {
        Query where = new Query(Criteria.where(ID).is(type));

        Update inc = new Update().inc(SEQ, nextStep(type));

        FindAndModifyOptions options = new FindAndModifyOptions();
        options.returnNew(true);
        options.upsert(true);

        return mongoTemplate.findAndModify(where, inc, options, Sequence.class);
    }

    private int nextStep(String type) {
        Integer nextStep = nextIncStep.get(type);
        if (nextStep == null) {
            nextIncStep.put(type, 1);
            return sequenceStep;
        }

        return nextStep;
    }

    @Data
    @Document(collection = SEQ_COL)
    private static class Sequence {

        @Field(ID)
        private String type;

        @Field(SEQ)
        private long seq;
    }
}
