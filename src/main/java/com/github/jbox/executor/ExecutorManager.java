package com.github.jbox.executor;

import com.github.jbox.executor.policy.CallerRunsPolicy;
import com.github.jbox.scheduler.ScheduleTask;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程池管理器
 *
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.3
 * @since 2017/1/16 14:15:00.
 */
public class ExecutorManager implements ExecutorLoggerInner {
    
    public static final ConcurrentMap<String, FlightRecorder> recorders = new ConcurrentHashMap<>();

    public static final ConcurrentMap<String, ExecutorService> executors = new ConcurrentHashMap<>();

    // ---- * ThreadPoolExecutor * ---- //

    public static ExecutorService newFixedMinMaxThreadPool(String group, int minPoolSize, int maxPoolSize,
                                                           int runnableQueueSize) {
        RejectedExecutionHandler rejectHandler = new CallerRunsPolicy(group);

        return newFixedMinMaxThreadPool(group, minPoolSize, maxPoolSize, runnableQueueSize, rejectHandler);
    }

    public static ExecutorService newFixedMinMaxThreadPool(String group, int minPoolSize, int maxPoolSize,
                                                           int runnableQueueSize,
                                                           RejectedExecutionHandler rejectHandler) {
        return executors.computeIfAbsent(group, (key) -> {
            BlockingQueue<Runnable> runnableQueue = new ArrayBlockingQueue<>(runnableQueueSize);

            ThreadFactory threadFactory = new NamedThreadFactory(group);

            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(minPoolSize, maxPoolSize,
                    ScheduleTask.HALF_AN_HOUR_INTERVAL, TimeUnit.MILLISECONDS,
                    runnableQueue,
                    threadFactory,
                    rejectHandler);

            return createExecutorProxy(group, threadPoolExecutor, ExecutorService.class);
        });
    }

    // ---- * newCachedThreadPool * ---- //
    public static ExecutorService newCachedThreadPool(String group) {
        return executors.computeIfAbsent(group, (key) -> {
            ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory(group));
            return createExecutorProxy(group, executor, ExecutorService.class);
        });
    }

    // ---- * newFixedThreadPool * ---- //
    public static ExecutorService newFixedThreadPool(String group, int poolSize) {
        return executors.computeIfAbsent(group, (key) -> {
            ExecutorService executor = Executors.newFixedThreadPool(poolSize, new NamedThreadFactory(group));
            return createExecutorProxy(group, executor, ExecutorService.class);
        });
    }

    // ---- * newScheduledThreadPool * ---- //
    public static ScheduledExecutorService newScheduledThreadPool(String group, int corePoolSize) {
        return (ScheduledExecutorService) executors.computeIfAbsent(group, (key) -> {

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(corePoolSize,
                    new NamedThreadFactory(group));

            return createExecutorProxy(group, executor, ScheduledExecutorService.class);
        });
    }

    // ---- * newSingleThreadExecutor * ---- //
    public static ExecutorService newSingleThreadExecutor(String group) {
        return executors.computeIfAbsent(group, (key) -> {
            ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory(group));
            return createExecutorProxy(group, executor, ExecutorService.class);
        });
    }

    // ---- * newSingleThreadScheduledExecutor * ---- //
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String group) {
        return (ScheduledExecutorService) executors.computeIfAbsent(group, (key) -> {

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                    new NamedThreadFactory(group));

            return createExecutorProxy(group, executor, ScheduledExecutorService.class);
        });
    }

    private static <T> T createExecutorProxy(String group, ExecutorService target, Class<T> interfaceType) {
        return interfaceType.cast(
                Proxy.newProxyInstance(
                        interfaceType.getClassLoader(),
                        new Class[]{interfaceType},
                        new RunnableDecoratorInterceptor(group, target)
                )
        );
    }

    @PreDestroy
    public void destroy() {
        executors.entrySet().stream()
                .filter(entry -> !entry.getValue().isShutdown())
                .forEach(entry -> {
                    entry.getValue().shutdown();
                    monitorLogger.info("executor [{}] is shutdown", entry.getKey());
                    executorLogger.info("executor [{}] is shutdown", entry.getKey());
                });
    }

    @Data
    @AllArgsConstructor
    public static final class FlightRecorder implements Serializable {

        private static final long serialVersionUID = -8342765829706151410L;

        private AtomicLong success;

        private AtomicLong failure;

        private AtomicLong totalRt;

        public FlightRecorder() {
            this.success = new AtomicLong(0L);
            this.failure = new AtomicLong(0L);
            this.totalRt = new AtomicLong(0L);
        }
    }
}
