package com.github.jbox.executor;

import com.github.jbox.executor.ExecutorManager.FlightRecorder;
import com.github.jbox.scheduler.ScheduleTask;
import com.github.jbox.stream.StreamForker;
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

    private Integer maxGroupSize;

    @Override
    public void schedule() throws Exception {
        List<Entry<String, ExecutorService>> executorSortedSet = executors.entrySet()
                .stream()
                .filter(entry -> !(entry.getValue() instanceof SyncInvokeExecutorService))
                .sorted((e1, e2) -> e2.getKey().length() - e1.getKey().length())
                .collect(Collectors.toList());

        StringBuilder logBuilder = new StringBuilder(128);
        // append group size:
        logBuilder.append("executor group size [").append(executorSortedSet.size()).append("]:\n");

        for (Map.Entry<String, ExecutorService> entry : executorSortedSet) {
            String group = entry.getKey();
            ThreadPoolExecutor executor = getThreadPoolExecutor(entry.getValue());
            if (executor == null) {
                continue;
            }

            Object[] recorder = getFlightRecorder(group);

            // append group detail:
            BlockingQueue<Runnable> queue = executor.getQueue();
            logBuilder.append(String.format(
                    "%-" + getMaxGroupSize(executorSortedSet) + "s > pool:[%s], active:[%d], core:[%d], max:[%d], "
                            + "success:[%s], failure:[%s], "
                            + "rt:[%s], tps:[%s], "
                            + "queued:[%d], remains:[%d]\n",

                    /*
                     * group
                     */
                    "'" + group + "'",
                    /*
                     *  pool detail
                     */
                    executor.getPoolSize(),
                    executor.getActiveCount(),
                    executor.getCorePoolSize(),
                    executor.getMaximumPoolSize(),

                    /*
                     * success, failure
                     */
                    numberFormat(recorder[0]),
                    numberFormat(recorder[1]),

                    /*
                     * rt, tps
                     */
                    String.format("%.2f", (double) recorder[2]),
                    numberFormat(calcTps(group, (long) recorder[3])),

                    /*
                     * runnable queue
                     */
                    queue.size(),
                    queue.remainingCapacity()
            ));

            // append task detail:
            StringBuilder[] taskDetailBuilder = getTaskDetail(queue);
            for (StringBuilder sb : taskDetailBuilder) {
                logBuilder.append(sb);
            }
        }

        monitorLogger.info(logBuilder.toString());
    }

    private int getMaxGroupSize(List<Entry<String, ExecutorService>> groupEntries) {
        if (maxGroupSize == null) {
            maxGroupSize = groupEntries.get(0).getKey().length() + "'' ".length();
        }
        return maxGroupSize;
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
                stringBuilder.append("  -> ");
                if (object != null) {
                    if (object instanceof AsyncRunnable || object instanceof AsyncCallable) {
                        Method taskInfoMethod = ReflectionUtils.findMethod(object.getClass(), "taskInfo");
                        ReflectionUtils.makeAccessible(taskInfoMethod);
                        stringBuilder
                                .append("task: ")
                                .append(ReflectionUtils.invokeMethod(taskInfoMethod, object))
                                .append(", obj: ")
                                .append(Objects.hashCode(object));
                    } else {
                        stringBuilder.append("task: ").append(ToStringBuilder.reflectionToString(object));
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

    private String numberFormat(Object obj) {
        return new DecimalFormat("##,###").format(obj);
    }

    @Override
    public long period() {
        return period;
    }


    /*
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Map<String, TaskScheduler> beans = applicationContext.getBeansOfType(TaskScheduler.class);
        if (beans == null || beans.isEmpty()) {
            RootBeanDefinition taskScheduler = new RootBeanDefinition(TaskScheduler.class);
            taskScheduler.setInitMethodName("start");
            taskScheduler.setDestroyMethodName("shutdown");
            taskScheduler.setScope(SCOPE_SINGLETON);
            taskScheduler.getConstructorArgumentValues().addIndexedArgumentValue(0, true);
            registry.registerBeanDefinition(getUsableBeanName("com.github.jbox.scheduler.TaskScheduler", registry),
                    taskScheduler);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }


    public void setPeriod(long period) {
        this.period = period;
    }

    private static String getUsableBeanName(String initBeanName, BeanDefinitionRegistry registry) {
        String beanName;
        int index = 0;
        do {
            beanName = initBeanName + "#" + index++;
        } while (registry.isBeanNameInUse(beanName));

        return beanName;
    }
    */
}
