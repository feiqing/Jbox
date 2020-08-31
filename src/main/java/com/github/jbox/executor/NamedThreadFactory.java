package com.github.jbox.executor;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jifang@alibaba-inc.com
 * @version 1.1
 * @since 2017/1/16 14:25:00.
 */
public class NamedThreadFactory implements ThreadFactory, ExecutorLoggerInner {

    private final AtomicInteger number = new AtomicInteger(0);

    private String group;

    public NamedThreadFactory(String group) {
        this.group = group;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(String.format("%s-%s", group, number.getAndIncrement()));
        thread.setUncaughtExceptionHandler(exceptionHandler);
        thread.setDaemon(false);
        return thread;
    }

    private static UncaughtExceptionHandler exceptionHandler = (t, e) -> {
        String message = String.format("thread: [%s]-[%s] runtime throws exception, state:[%s]",
            t.getName(), t.getId(), t.getState());
        monitorLogger.error("{}", message, e);
        executorLogger.error("{}", message, e);
    };
}
