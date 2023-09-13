package com.alibaba.jbox.scheduler;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 实现该接口并注册为SpringBean由{@link TaskScheduler}自动发现并注册, 自动调度.
 *
 * @author jifang
 * @version 1.2:自动注册, 1.3:可以选择不自动注册
 * @since 16/10/20 上午8:15.
 */
public interface ScheduleTask {

    AtomicReference<ExecutorService> defaultExecutor = new AtomicReference<>(null);

    void schedule() throws Exception;

    long period();

    default ExecutorService executor() {
        if (defaultExecutor.get() != null) {
            return defaultExecutor.get();
        }

        synchronized (defaultExecutor) {
            if (defaultExecutor.get() == null) {
                BasicThreadFactory factory = new BasicThreadFactory.Builder().namingPattern("J-Schedule-Default-Exec-%d").daemon(true).build();
                defaultExecutor.set(Executors.newCachedThreadPool(factory));
            }
        }

        return defaultExecutor.get();
    }

    default boolean autoRegistered() {
        return true;
    }

    default String desc() {
        return this.getClass().getName();
    }
}
