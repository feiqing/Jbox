package com.github.jbox.kafka.helper;

import com.github.jbox.executor.ExecutorLoggerInner;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/22 2:27 PM.
 */
public class ThreadManager {

    private static final ConcurrentMap<String, AtomicInteger> number = new ConcurrentHashMap<>();

    private static final Queue<Thread> threads = new ConcurrentLinkedQueue<>();

    public static void newThread(String group, String topic, Runnable r) {
        Thread thread = new NamedThreadFactory(group, topic).newThread(r);
        threads.add(thread);
    }

    public static void startAll() {
        threads.forEach(Thread::start);
    }

    public static void stopAll() {
        threads.forEach(Thread::interrupt);
    }

    private static class NamedThreadFactory implements ThreadFactory, ExecutorLoggerInner {

        private String group;

        private String topic;

        NamedThreadFactory(String group, String topic) {
            this.group = group;
            this.topic = topic;
        }

        @Override
        public Thread newThread(Runnable r) {
            int number = ThreadManager.number.computeIfAbsent(group + "-" + topic, (_K) -> new AtomicInteger(0)).getAndIncrement();
            Thread thread = new Thread(r);
            thread.setName(String.format("%s[%s]-%s", group, topic, number));
            thread.setUncaughtExceptionHandler(exceptionHandler);
            thread.setDaemon(false);

            return thread;
        }

        private Thread.UncaughtExceptionHandler exceptionHandler = (t, e) -> {
            String message = String.format("thread: [%s]-[%s] runtime throws exception, state:[%s]",
                    t.getName(), t.getId(), t.getState());
            monitorLogger.error("{}", message, e);
            executorLogger.error("{}", message, e);
        };
    }
}
