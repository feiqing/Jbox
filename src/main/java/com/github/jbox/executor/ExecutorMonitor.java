package com.github.jbox.executor;

import com.github.jbox.executor.ExecutorManager.FlightRecorder;
import com.github.jbox.scheduler.ScheduleTask;
import com.github.jbox.utils.Collections3;
import com.github.jbox.utils.JboxUtils;
import com.github.jbox.utils.ProxyTargetUtils;
import com.github.jbox.utils.T;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.github.jbox.executor.ExecutorManager.executors;
import static com.github.jbox.executor.ExecutorManager.recorders;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.5
 * @since 2017/8/22 15:32:00.
 */
public class ExecutorMonitor implements ScheduleTask, ExecutorLoggerInner {

    private static final String placeholder = "-";

    private static final Class<?> runnableAdapterType = Executors.callable(() -> {
    }, null).getClass();

    private static Map<String, AtomicLong> beforeInvoked = new HashMap<>();

    @Setter
    private long period = T.OneM;

    @Setter
    private int taskDetailsTopN = 5;

    @Override
    public void schedule() {

        List<Entry<String, ExecutorService>> jBoxExecutors = executors.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith("com.github.jbox"))
                .sorted((e1, e2) -> e2.getKey().length() - e1.getKey().length())
                .collect(Collectors.toList());

        List<Entry<String, ExecutorService>> bizExecutors = executors.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().startsWith("com.github.jbox"))
                .sorted((e1, e2) -> e2.getKey().length() - e1.getKey().length())
                .collect(Collectors.toList());


        int size = jBoxExecutors.size() + bizExecutors.size();

        monitorLogger.trace("executors size [{}], details:(|active,core,pool,max|success,failed|rt,tps|queued,remain|)", size);

        int maxGroupSize = Math.max(getMaxGroupSize(jBoxExecutors), getMaxGroupSize(bizExecutors));
        for (Map.Entry<String, ExecutorService> entry : jBoxExecutors) {
            logGroupDetail(entry.getKey(), entry.getValue(), maxGroupSize);
        }

        for (Map.Entry<String, ExecutorService> entry : bizExecutors) {
            logGroupDetail(entry.getKey(), entry.getValue(), maxGroupSize);
        }
    }

    private void logGroupDetail(String group, ExecutorService executorService, int maxGroupSize) {
        Optional<ThreadPoolExecutor> executor = getThreadPoolExecutor(executorService);

        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append(String.format("%-" + maxGroupSize + "s", group));
        logBuilder.append(" |").append(executor.isPresent() ? executor.get().getActiveCount() : placeholder)
                .append(",").append(executor.isPresent() ? executor.get().getCorePoolSize() : placeholder)
                .append(",").append(executor.isPresent() ? executor.get().getPoolSize() : placeholder)
                .append(",").append(executor.isPresent() ? executor.get().getMaximumPoolSize() : placeholder);


        Object[] recorder = getFlightRecorder(group);
        logBuilder.append("|").append(recorder[0])
                .append(",").append(recorder[1])
                .append("|").append(String.format("%.2f", (double) recorder[2]))
                .append(",").append(calcTps(group, (long) recorder[3]));

        BlockingQueue<Runnable> queue = executor.map(ThreadPoolExecutor::getQueue).orElse(null);
        logBuilder.append("|").append(queue != null ? queue.size() : placeholder)
                .append(",").append(queue != null ? queue.remainingCapacity() : placeholder)
                .append("|");

        logBuilder.append(buildTaskDetail(queue));

        monitorLogger.info("{}", logBuilder);
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
        return period * 1.0 / T.OneS;
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

    private StringBuilder buildTaskDetail(BlockingQueue<Runnable> queue) {
        StringBuilder sb = new StringBuilder();
        if (Collections3.size(queue) == 0) {
            return sb;
        }

        Iterator<Runnable> iterator = queue.iterator();
        int topNum = 0;


        while (topNum++ < taskDetailsTopN && iterator.hasNext()) {
            Runnable runnable = iterator.next();
            Object task;
            if (runnable instanceof FutureTask) {
                task = getFutureTaskInnerAsyncObject(runnable);
            } else {
                task = runnable;
            }

            sb.append('\n').append(" -> ");
            if (task == null) {
                sb.append(placeholder);
            } else if (task instanceof AsyncRunnable) {
                sb.append(((AsyncRunnable) task).taskInfo()).append("(").append(task.hashCode()).append(")");
            } else if (task instanceof AsyncCallable) {
                sb.append(((AsyncCallable) task).taskInfo()).append("(").append(task.hashCode()).append(")");
            } else {
                sb.append(ToStringBuilder.reflectionToString(task));
            }
        }

        return sb;
    }

    private Map<ExecutorService, Optional<ThreadPoolExecutor>> cache = new ConcurrentHashMap<>();

    private Optional<ThreadPoolExecutor> getThreadPoolExecutor(ExecutorService executorProxy) {
        return cache.computeIfAbsent(executorProxy, _k -> {
            ThreadPoolExecutor executor = null;
            if (executorProxy instanceof ThreadPoolExecutor) {
                executor = (ThreadPoolExecutor) executorProxy;
            } else if (Proxy.isProxyClass(executorProxy.getClass())) {
                Object target = ProxyTargetUtils.getJdkProxyTarget(executorProxy);
                if (target == null) {
                    executor = null;
                } else if (target instanceof ThreadPoolExecutor) {
                    executor = (ThreadPoolExecutor) target;
                } else {
                    executor = (ThreadPoolExecutor) JboxUtils.getFieldValue(target, "e");
                }
            }

            return Optional.ofNullable(executor);
        });
    }

    private Object getFutureTaskInnerAsyncObject(Object futureTask) {
        Object callable = JboxUtils.getFieldValue(futureTask, "callable");
        if (callable != null) {
            if (callable instanceof AsyncCallable) {
                return callable;
            } else if (callable instanceof AsyncRunnable) {
                return callable;
            } else if (runnableAdapterType.isAssignableFrom(callable.getClass())) {
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
