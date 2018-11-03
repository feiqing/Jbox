package com.github.jbox.mongo;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.jbox.mongo.TableConstant.*;

/**
 * 每次启动, Sequence抬升1000
 *
 * @author cunxiao
 * @since 2018-02-28 17:03:00.
 **/
public class SequenceDAO {

    private static final ConcurrentMap<String, SequenceRange> ranges = new ConcurrentHashMap<>();

    private static final long DEFAULT_STEP = 1000L;

    private long step;

    private MongoOperations mongoTemplate;

    public SequenceDAO(MongoOperations mongoTemplate) {
        this(DEFAULT_STEP, mongoTemplate);
    }

    public SequenceDAO(long step, MongoOperations mongoTemplate) {
        this.step = step;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 生成一个自增ID
     *
     * @param type: 表名
     */
    public long generateId(String type) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(type), "sequence name can not be empty.");

        SequenceRange range = getRange(type);
        long value = range.getAndIncrement();

        if (value == -1) {
            synchronized (ranges) {
                for (; ; ) {
                    if (range.isOver()) {
                        range = nextRange(type);
                        ranges.put(type, range);
                    }

                    value = range.getAndIncrement();
                    if (value == -1) {
                        continue;
                    }

                    break;
                }
            }
        }

        if (value < 0) {
            throw new SequenceException("Sequence value overflow, value = " + value);
        }

        return value;

    }

    private SequenceRange getRange(String type) {
        SequenceRange range = ranges.get(type);
        if (range == null) {
            synchronized (ranges) {
                if ((range = ranges.get(type)) == null) {
                    range = nextRange(type);
                    ranges.put(type, range);
                }
            }
        }

        return range;
    }

    private SequenceRange nextRange(String type) {
        Query where = new Query(Criteria.where(ID).is(type));
        Update inc = new Update().inc(SEQ, step);

        FindAndModifyOptions options = new FindAndModifyOptions();
        options.returnNew(true);
        options.upsert(true);

        SequenceDO sequence = mongoTemplate.findAndModify(where, inc, options, SequenceDO.class);
        if (sequence == null) {
            throw new SequenceException("could not find new Sequence.");
        }

        long newValue = sequence.getSeq();
        long oldValue = newValue - step + 1;

        return new SequenceRange(oldValue, newValue);
    }

    public class SequenceRange {

        @Getter
        private final long min;

        @Getter
        private final long max;

        private final AtomicLong seq;

        @Getter
        private volatile boolean over = false;

        SequenceRange(long min, long max) {
            this.min = min;
            this.max = max;
            this.seq = new AtomicLong(min);
        }

        long getAndIncrement() {
            if (over) {
                return -1;
            }

            long currentValue = seq.getAndIncrement();
            if (currentValue > max) {
                over = true;
                return -1;
            }

            return currentValue;
        }

        @Override
        public String toString() {
            return "max: " + max + ", min: " + min + ", seq: " + seq;
        }
    }

    @Data
    @Document(collection = SEQ_COL)
    private static class SequenceDO {

        @Field(ID)
        private String type;

        @Field(SEQ)
        private long seq;
    }
}
