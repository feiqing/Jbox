package com.github.jbox.executor;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PreDestroy;

import com.github.jbox.executor.policy.CallerRunsPolicy;

import com.github.jbox.scheduler.ScheduleTask;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 线程池管理器:
 * 1. 优势:
 * - 1) 每个分组{@code group}中的线程默认都是单例的, 防止出现在方法内部循环创建线程池的错误写法;
 * - 2) 线程以{@code '${group}-${number}'}形式命名, 使在查看线程栈时更加清晰;
 * - 3) 开放{@code newFixedMinMaxThreadPool()}方法, 提供比{@code Executors}更灵活, 比{@code ThreadPoolExecutor}更便捷的配置方式;
 * - 4) 提供{@code com.github.jbox.executor.policy}线程拒绝策略, 在{@code RunnableQueue}满时打印日志;
 * - 5) 添加{@code ExecutorMonitor}监控: 将线程池监控日志打印到{@code 'executor-monitor'}这个{@code Logger}下, 打印内容包含:
 * * -> 5.a) 线程组信息: 'group', 'pool count', 'active count', 'core pool count', 'max pool count'
 * * -> 5.b) 线程组执行信息: 'success', 'failure', 'rt', 'tps'
 * * -> 5.c) RunnableQueue信息: 'queues:被阻塞在Queue内任务数量', 'remains:Queue尚余空间'
 * * -> 5.d) 被阻塞的任务detail信息: 'taskInfo()', 实例id
 *
 * 2. 封装{@code java.lang.Runnable}为{@code AsyncRunnable}, 描述信息详见{@link AsyncRunnable};
 * 3. 封装{@code java.util.concurrent.Callable}为{@code AsyncCallable}, 描述信息详见{@link AsyncCallable};
 * 4. 可通过{@code setSyncInvoke()}方法将提交到线程池内的task在主线程中同步执行, 便于debug业务逻辑或其他场景, 详见{@link SyncInvokeExecutorService},
 * 目前暂时不支持{@code ScheduledExecutorService}实现;
 * 5. 如果将{@code ExecutorManager}注册为SpringBean, 会在应用关闭时自动将线程池关闭掉, 防止线程池未关导致应用下线不成功的bug;
 *
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.3
 * @since 2017/1/16 14:15:00.
 */
public class ExecutorManager implements ExecutorLoggerInner {

    private static final String SYNC_PATTERN = "sync-%s";

    static ConcurrentMap<String, FlightRecorder> recorders = new ConcurrentHashMap<>();

    static ConcurrentMap<String, ExecutorService> executors = new ConcurrentHashMap<>();

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
        return (ScheduledExecutorService)executors.computeIfAbsent(group, (key) -> {

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
        return (ScheduledExecutorService)executors.computeIfAbsent(group, (key) -> {

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory(group));

            return createExecutorProxy(group, executor, ScheduledExecutorService.class);
        });
    }

    private static <T> T createExecutorProxy(String group, ExecutorService target, Class<T> interfaceType) {
        if (isSyncInvoke(group) && !(target instanceof ScheduledExecutorService)) {
            target.shutdownNow();
            return interfaceType.cast(new SyncInvokeExecutorService());
        }

        return interfaceType.cast(
            Proxy.newProxyInstance(
                interfaceType.getClass().getClassLoader(),
                new Class[] {interfaceType},
                new RunnableDecoratorInterceptor(group, target)
            )
        );
    }

    private static boolean isSyncInvoke(String group) {
        boolean syncAll = Boolean.getBoolean(String.format(SYNC_PATTERN, "all"));
        if (syncAll) {
            return true;
        }

        return Boolean.getBoolean(String.format(SYNC_PATTERN, group));
    }

    public static void setSyncInvoke(String group, boolean sync) {
        System.setProperty(String.format(SYNC_PATTERN, group), String.valueOf(sync));
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
    static final class FlightRecorder implements Serializable {

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
