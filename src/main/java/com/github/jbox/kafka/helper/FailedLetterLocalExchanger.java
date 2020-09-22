package com.github.jbox.kafka.helper;

import com.alibaba.fastjson.JSONObject;
import com.github.jbox.utils.Collections3;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/22 1:00 PM.
 */
public class FailedLetterLocalExchanger {

    private final ConcurrentMap<String, List<SynchronousQueue<ConsumerRecord<String, JSONObject>>>> transQueues = new ConcurrentHashMap<>();

    private final BlockingQueue<ConsumerRecord<String, JSONObject>> letterQueue = new LinkedBlockingQueue<>();

    {
        ThreadManager.newThread("FailedLetterLocalExchanger", "default", () -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            ConsumerRecord<String, JSONObject> letter = letterQueue.take();

                            List<SynchronousQueue<ConsumerRecord<String, JSONObject>>> queues = transQueues.get(letter.topic());
                            if (Collections3.isEmpty(queues)) {
                                continue;
                            }

                            queues.get(Math.abs(letter.partition()) % queues.size()).put(letter);
                        } catch (InterruptedException ie) {
                            return;
                        }
                    }
                }
        );
    }

    public void addQueue(String topic, SynchronousQueue<ConsumerRecord<String, JSONObject>> queue) {
        this.addQueues(topic, Collections.singletonList(queue));
    }

    public void addQueues(String topic, List<SynchronousQueue<ConsumerRecord<String, JSONObject>>> queues) {
        transQueues.put(topic, queues);
    }

    public void addLetter(ConsumerRecord<String, JSONObject> msg) {
        letterQueue.add(msg);
    }
}
