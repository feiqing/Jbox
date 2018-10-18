package com.github.jbox.executor;

import com.github.jbox.executor.ExecutorManager.FlightRecorder;
import com.github.jbox.scheduler.ScheduleTask;
import com.github.jbox.stream.StreamForker;
import com.github.jbox.utils.Collections3;
import com.github.jbox.utils.JboxUtils;
import com.github.jbox.utils.ProxyTargetUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.jbox.executor.ExecutorManager.executors;
import static com.github.jbox.executor.ExecutorManager.recorders;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.5
 * @since 2017/8/22 15:32:00.
 */
public class ExecutorMonitor implements ScheduleTask, ExecutorLoggerInner {

    private static final Class<?> RUNNABLE_ADAPTER_CLASS = Executors.callable(() -> {
    }, null).getClass();

    private static Map<String, AtomicLong> beforeInvoked = new HashMap<>();

    private static final String ASYNC_KEY = "async";

    private static final String FUTURE_KEY = "future";

    private static final String CALLABLE_KEY = "callable";

    private long period = _1M_INTERVAL;

    @Override
    public void schedule() {

        List<Entry<String, ExecutorService>> jBoxExecutors = executors.entrySet()
                .stream()
                .filter(entry -> !(entry.getValue() instanceof SyncInvokeExecutorService))
                .filter(entry -> entry.getKey().startsWith("com.github.jbox"))
                .sorted((e1, e2) -> e2.getKey().length() - e1.getKey().length())
                .collect(Collectors.toList());

        List<Entry<String, ExecutorService>> bizExecutors = executors.entrySet()
                .stream()
                .filter(entry -> !(entry.getValue() instanceof SyncInvokeExecutorService))
                .filter(entry -> !entry.getKey().startsWith("com.github.jbox"))
                .sorted((e1, e2) -> e2.getKey().length() - e1.getKey().length())
                .collect(Collectors.toList());


        int size = jBoxExecutors.size() + bizExecutors.size();

        StringBuilder logBuilder = new StringBuilder(36 * size);
        // append group size:
        logBuilder.append("executors size:")
                .append(size)
                .append(", details:(")
                .append("|pool,active,core,max|success,failed|rt,tps|queued,remain|")
                .append(")\n");

        int maxGroupSize = Math.max(getMaxGroupSize(jBoxExecutors), getMaxGroupSize(bizExecutors));
        for (Map.Entry<String, ExecutorService> entry : jBoxExecutors) {
            buildLog(entry.getKey(), entry.getValue(), logBuilder, maxGroupSize);
        }

        for (Map.Entry<String, ExecutorService> entry : bizExecutors) {
            buildLog(entry.getKey(), entry.getValue(), logBuilder, maxGroupSize);
        }

        monitorLogger.info(logBuilder.toString());
    }

    // group |pool,active,core,max|success,failed|rt,tps|queued,remain|
    // com.github.jbox.scheduler:TaskScheduler |1,0,1,2147483647|613,0|0.03,10|1,2147483647|
    // JobDispatcher2                          |1,0,1,2147483647|613,0|0.03,10|1,2147483647|
    private void buildLog(String group, ExecutorService executorService, StringBuilder logBuilder, int maxGroupSize) {
        ThreadPoolExecutor executor = getThreadPoolExecutor(executorService);
        if (executor == null) {
            return;
        }

        logBuilder.append(String.format("%-" + maxGroupSize + "s", group));
        logBuilder.append(" |").append(executor.getPoolSize())
                .append(",").append(executor.getActiveCount())
                .append(",").append(executor.getCorePoolSize())
                .append(",").append(executor.getMaximumPoolSize());


        Object[] recorder = getFlightRecorder(group);
        logBuilder.append("|").append(recorder[0])
                .append(",").append(recorder[1])
                .append("|").append(String.format("%.2f", (double) recorder[2]))
                .append(",").append(calcTps(group, (long) recorder[3]));

        BlockingQueue<Runnable> queue = executor.getQueue();
        logBuilder.append("|").append(queue.size())
                .append(",").append(queue.remainingCapacity())
                .append("|\n");

        // append task detail:
        StringBuilder[] taskDetailBuilder = getTaskDetail(queue);
        for (StringBuilder sb : taskDetailBuilder) {
            logBuilder.append(sb);
        }
    }

    private int getMaxGroupSize(List<Entry<String, ExecutorService>> groupEntries) {
        return Collections3.isNotEmpty(groupEntries) ? groupEntries.get(0).getKey().length() : 0;
    }

    private long calcTps(String group, long invoked) {
        long before = beforeInvoked.computeIfAbsent(group, (k) -> new AtomicLong(0L))
                .getAndSet(invoked);
        return (long) ((invoked - before) / passedSeconds());
    }

    private double passedSeconds() {
        return period * 1.0 / _1S_INTERVAL;
    }

    private Object[] getFlightRecorder(String group) {
        FlightRecorder recorder = recorders.getOrDefault(group, new FlightRecorder());
        long success = recorder.getSuccess().get();
        long failure = recorder.getFailure().get();
        double rt;
        if (success == 0 && failure == 0) {
            rt = 0.0;
        } else {
            rt = recorder.getTotalRt().get() * 1.0 / (success + failure);
        }

        return new Object[]{success, failure, rt, success + failure};
    }

    private StringBuilder[] getTaskDetail(BlockingQueue<Runnable> queue) {
        StreamForker<Runnable> forker = new StreamForker<>(queue.stream())
                .fork(ASYNC_KEY, stream -> stream
                        .filter(runnable -> runnable instanceof AsyncRunnable)
                        .collect(new Collector()))
                .fork(CALLABLE_KEY, stream -> stream
                        .filter(callable -> callable instanceof AsyncCallable)
                        .collect(new Collector()))
                .fork(FUTURE_KEY, stream -> stream
                        .filter(runnable -> runnable instanceof FutureTask)
                        .map(this::getFutureTaskInnerAsyncObject)
                        .collect(new Collector()));

        StreamForker.Results results = forker.getResults();
        StringBuilder asyncLogBuilder = results.get(ASYNC_KEY);
        StringBuilder callableLogBuilder = results.get(CALLABLE_KEY);
        StringBuilder futureLogBuilder = results.get(FUTURE_KEY);

        return new StringBuilder[]{asyncLogBuilder, callableLogBuilder, futureLogBuilder};
    }

    /**
     * @param executorProxy
     * @return
     * @since 1.1
     */
    private ThreadPoolExecutor getThreadPoolExecutor(ExecutorService executorProxy) {
        ThreadPoolExecutor executor = null;

        if (executorProxy instanceof ThreadPoolExecutor) {
            executor = (ThreadPoolExecutor) executorProxy;
        } else if (Proxy.isProxyClass(executorProxy.getClass())) {
            Object target = ProxyTargetUtils.getJdkProxyTarget(executorProxy);
            if (target == null) {
                executor = null;
            } else if (target instanceof ThreadPoolExecutor) {
                executor = (ThreadPoolExecutor) target;
            } else if (target instanceof SyncInvokeExecutorService) {
                executor = null;
            } else {
                executor = (ThreadPoolExecutor) JboxUtils.getFieldValue(target, "e");
            }
        }

        return executor;
    }

    /**
     * @since 1.2
     */
    private class Collector implements java.util.stream.Collector<Object, StringBuilder, StringBuilder> {

        @Override
        public Supplier<StringBuilder> supplier() {
            return StringBuilder::new;
        }

        @Override
        public BiConsumer<StringBuilder, Object> accumulator() {
            return (stringBuilder, object) -> {
                stringBuilder.append(" -> ");
                if (object != null) {
                    if (object instanceof AsyncRunnable || object instanceof AsyncCallable) {
                        Method taskInfoMethod = ReflectionUtils.findMethod(object.getClass(), "taskInfo");
                        ReflectionUtils.makeAccessible(taskInfoMethod);
                        stringBuilder
                                .append(ReflectionUtils.invokeMethod(taskInfoMethod, object))
                                .append("(")
                                .append(Objects.hashCode(object))
                                .append(")");

                    } else {
                        stringBuilder.append(ToStringBuilder.reflectionToString(object));
                    }
                } else {
                    stringBuilder.append("null");
                }
                stringBuilder.append("\n");
            };
        }

        @Override
        public BinaryOperator<StringBuilder> combiner() {
            return StringBuilder::append;
        }

        @Override
        public Function<StringBuilder, StringBuilder> finisher() {
            return Function.identity();
        }

        @Override
        public Set<Characteristics> characteristics() {
            return EnumSet.of(Characteristics.CONCURRENT, Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH);
        }
    }

    private Object getFutureTaskInnerAsyncObject(Object futureTask) {
        Object callable = JboxUtils.getFieldValue(futureTask, "callable");
        if (callable != null) {
            if (callable instanceof AsyncCallable) {
                return callable;
            } else if (callable instanceof AsyncRunnable) {
                return callable;
            } else if (RUNNABLE_ADAPTER_CLASS.isAssignableFrom(callable.getClass())) {
                return JboxUtils.getFieldValue(callable, "task");
            }
        }
        return null;
    }

    @Override
    public long period() {
        return period;
    }
}
