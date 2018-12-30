package com.github.jbox.scheduler;

import com.github.jbox.executor.ExecutorManager;
import com.github.jbox.executor.policy.DiscardOldestPolicy;
import lombok.Data;
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

    Logger SCHEDULE_TASK_LOGGER = LoggerFactory.getLogger("task-scheduler");

    long _1S_INTERVAL = 1000L;

    long _10S_INTERVAL = 10 * _1S_INTERVAL;

    long _30S_INTERVAL = 3 * _10S_INTERVAL;

    long _1M_INTERVAL = 2 * _30S_INTERVAL;

    long HALF_AN_HOUR_INTERVAL = 30 * _1M_INTERVAL;

    long ONE_HOUR_INTERVAL = 2 * HALF_AN_HOUR_INTERVAL;

    long _12_HOUR_INTERVAL = 12 * ONE_HOUR_INTERVAL;

    long ONE_DAY_INTERVAL = 2 * _12_HOUR_INTERVAL;

    void schedule() throws Exception;

    Configuration configuration();

    default String taskDesc() {
        return this.getClass().getName();
    }

    @Data
    class Configuration {

        // 静态参数: 不能变
        private static final String group = "com.github.jbox.scheduler:ScheduleTask";

        private static final int minSize = 3;

        private static final int maxSize = 5;

        private static final int queueSize = 36;

        private static final ExecutorService defaultExecutor = ExecutorManager.newFixedMinMaxThreadPool(group, minSize, maxSize, queueSize, new DiscardOldestPolicy(group));

        // 动态参数: 可配置
        private long period;

        private ExecutorService executor = defaultExecutor;

        private boolean invokeAtStart = false;

        private boolean autoRegistered = true;

        public Configuration(long period) {
            this.period = period;
        }
    }
}
