package com.github.jbox.scheduler;

import com.github.jbox.executor.ExecutorManager;
import com.github.jbox.executor.policy.DiscardOldestPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * 实现该接口并注册为SpringBean由{@link TaskScheduler}自动发现并注册, 自动调度.
 *
 * @author jifang
 * @version 1.2:自动注册, 1.3:可以选择不自动注册
 * @since 16/10/20 上午8:15.
 */
public interface ScheduleTask {

    String group = "com.github.jbox.scheduler:ScheduleTask";

    int minSize = 3;

    int maxSize = 5;

    int queueSize = 36;

    ExecutorService defaultExecutor = ExecutorManager.newFixedMinMaxThreadPool(group, minSize, maxSize, queueSize, new DiscardOldestPolicy(group));

    Logger SCHEDULE_TASK_LOGGER = LoggerFactory.getLogger("task-scheduler");

    void schedule() throws Exception;

    long period();

    default ExecutorService executor() {
        return defaultExecutor;
    }

    default boolean autoRegistered() {
        return true;
    }

    default String taskDesc() {
        return this.getClass().getName();
    }
}
