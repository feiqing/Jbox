package com.github.jbox.oplog;

import com.github.jbox.executor.ExecutorManager;
import com.github.jbox.utils.T;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.mongodb.BasicDBObject;
import com.mongodb.CursorType;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static com.github.jbox.utils.DateUtils.timeFormat;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/22 8:37 PM.
 */
@Slf4j(topic = "OplogReader")
class OplogReader {

    private static final String CURSOR_TABLE = "batis.cursor";

    private static final String CURSOR_KEY = "cursorV1";

    private static final AtomicBoolean started = new AtomicBoolean(false);

    private static final ConcurrentMap<String, MongoClient> mongos = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, AtomicLong> mongo2cnt = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, ConcurrentMap<String, BsonTimestamp>> mongo2ts = new ConcurrentHashMap<>();

    private static final EventTranslatorOneArg<Oplog, Object> translator = (event, sequence, arg) -> {
        Object[] args = (Object[]) arg;
        event.setUrl((String) args[0]);
        event.setDb((String) args[1]);
        event.setNs((String) args[2]);
        event.setOp((String) args[3]);
        event.setWhere((Document) args[4]);
        event.setData((Document) args[5]);
        event.setTs((BsonTimestamp) args[6]);
    };

    void start(List<Mongo> mongos) {
        started.set(true);

        // reader worker
        ExecutorService oplogReaderWorker = ExecutorManager.newFixedThreadPool("OplogReaderWorker", mongos.size());
        for (Mongo mongo : mongos) {
            Runnable task = new OplogReadTask(
                    getMongoClient(mongo.getUrl()),
                    mongo.getUrl(),
                    mongo.getDb()
            );

            oplogReaderWorker.submit(task);
        }

        // cursor sync worker
        ScheduledExecutorService oplogCursorSyncWorker = ExecutorManager.newSingleThreadScheduledExecutor("OplogCursorSyncWorker");
        oplogCursorSyncWorker.scheduleAtFixedRate(new OplogTsSynchTask(), 0, 10, TimeUnit.SECONDS);
    }

    void stop() {
        started.set(false);
        mongos.values().forEach(MongoClient::close);
    }


    @AllArgsConstructor
    private class OplogReadTask implements Runnable {

        private MongoClient client;

        private String url;

        private String db;

        @Override
        public void run() {
            while (started.get()) {
                try {
                    BasicDBObject where = new BasicDBObject();
                    where.put("ts", new BasicDBObject("$gt", getStartCursor(client, url, db)));
                    readLog(client, url, db, where);
                } catch (Throwable t) {
                    log.error("mongo [{}/{}] read oplog error.", url, db, t);
                }
            }
        }
    }

    private void readLog(MongoClient client, String url, String db, BasicDBObject where) {

        int batchSize = OplogTailStarter.config.getLogBatchSize();

        Set<String> ops = OplogTailStarter.config.getOps();

        List<Predicate<String>> exps = OplogTailStarter.config.getTables();
        Iterable<Document> cursor = client.getDatabase("local").getCollection("oplog.rs")
                .find(where)
                .sort(new BasicDBObject("$natural", 1))
                .cursorType(CursorType.Tailable)
                .cursorType(CursorType.TailableAwait)
                .noCursorTimeout(true)
                .batchSize(batchSize);

        for (Document opLog : cursor) {
            BsonTimestamp ts = (BsonTimestamp) opLog.get("ts");
            String op = opLog.getString("op");
            String ns = opLog.getString("ns");

            mongo2ts.computeIfAbsent(url, (_K) -> new ConcurrentHashMap<>()).put(db, ts);

            if (!ops.contains(op)) {
                continue;
            }
            if (!isNeedPutLog(ns, exps)) {
                continue;
            }

            OplogDisruptor.getDisruptor(url).publishEvent(translator, new Object[]{url, db, ns, op, opLog.get("o2"), opLog.get("o"), ts});
            mongo2cnt.computeIfAbsent(url, (_K) -> new AtomicLong(0L)).addAndGet(1);
        }
    }


    private class OplogTsSynchTask implements Runnable {

        @Override
        public void run() {
            for (Map.Entry<String, ConcurrentMap<String, BsonTimestamp>> entry1 : OplogReader.mongo2ts.entrySet()) {
                String mongo = entry1.getKey();

                for (Map.Entry<String, BsonTimestamp> entry2 : entry1.getValue().entrySet()) {
                    String db = entry2.getKey();
                    BsonTimestamp cursor = entry2.getValue();

                    MongoDatabase database = getMongoClient(mongo).getDatabase(db);

                    database.getCollection(CURSOR_TABLE).updateOne(
                            new BasicDBObject("_id", CURSOR_KEY),
                            new BasicDBObject("$set", new BasicDBObject("cursor", cursor)),
                            new UpdateOptions().upsert(true)
                    );

                    long systime = T.ts(database.runCommand(new BasicDBObject("serverStatus", 1)).getDate("localTime").getTime());

                    long lag = (systime - (long) cursor.getTime() * 1000) / 1000;
                    log.info("sync mongo:[{}] ts:[{}] lag:[{}]s cnt:[{}] to {}.", mongo, timeFormat((long) cursor.getTime() * 1000), lag, mongo2cnt.get(mongo), CURSOR_TABLE);
                }
            }
        }
    }

    private BsonTimestamp getStartCursor(MongoClient client, String mongo, String db) {
        BsonTimestamp ts = null;
        FindIterable<Document> iterable = client.getDatabase(db).getCollection(CURSOR_TABLE).find(new BasicDBObject("_id", CURSOR_KEY));
        for (Document document : iterable) {
            ts = (BsonTimestamp) document.get("cursor");
        }
        if (ts == null) {
            ts = new BsonTimestamp((int) ((T.tm(System.currentTimeMillis()) - 2 * T.OneH) / 1000), 0);
//            ts = new BsonTimestamp();
        }

        log.info("mongo:[{}] start at: [{}]", mongo, timeFormat((long) ts.getTime() * 1000));
        return ts;
    }

    private static boolean isNeedPutLog(String ns, List<Predicate<String>> exps) {
        if (exps == null || exps.isEmpty()) {
            return true;
        }

        String table = StringUtils.substringAfter(ns, ".");
        for (Predicate<String> exp : exps) {
            if (exp.test(table)) {
                return true;
            }
        }

        return false;
    }

    private static MongoClient getMongoClient(String mongoUrl) {
        return mongos.computeIfAbsent(mongoUrl, (_K) -> {
            MongoClientURI uri = new MongoClientURI(mongoUrl);
            return new MongoClient(uri);
        });
    }
}
