package com.github.jbox.executor;

import com.github.jbox.executor.ExecutorManager.FlightRecorder;
import com.github.jbox.helpers.ExceptionableSupplier;
import com.google.common.collect.Sets;
import lombok.NonNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static com.github.jbox.executor.ExecutorManager.recorders;
import static com.github.jbox.utils.JboxUtils.runWithNewMdcContext;

/**
 * @author jifang@alibaba-inc.com
 * @version 1.1
 * @since 2017/1/16 下午3:42.
 */
class RunnableDecoratorInterceptor implements InvocationHandler {

    private static final Set<String> NEED_PROXY_METHODS = Sets.newConcurrentHashSet(Arrays.asList(
            "execute",
            "submit",
            "schedule",
            "invokeAll",/*todo*/
            "scheduleAtFixedRate",
            "scheduleWithFixedDelay"
    ));

    private String group;

    private ExecutorService target;

    RunnableDecoratorInterceptor(String group, ExecutorService target) {
        this.group = group;
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (NEED_PROXY_METHODS.contains(methodName)) {
            Object firstArg = args[0];
            FlightRecorder recorder = recorders.computeIfAbsent(group, (k) -> new FlightRecorder());
            AsyncContext context = AsyncContext.newContext(group);
            if (firstArg instanceof AsyncRunnable) {
                args[0] = new AsyncRunnableDecorator(context, recorder, (AsyncRunnable) firstArg);
            } else if (firstArg instanceof AsyncCallable) {
                args[0] = new AsyncCallableDecorator(context, recorder, (AsyncCallable) firstArg);
            } else if (firstArg instanceof Runnable) {
                args[0] = new RunnableDecorator(context, recorder, (Runnable) firstArg);
            } else if (firstArg instanceof Callable) {
                args[0] = new CallableDecorator(context, recorder, (Callable) firstArg);
            }
        }

        return method.invoke(target, args);
    }
}

class RunnableDecorator implements AsyncRunnable {

    private AsyncContext context;

    private FlightRecorder recorder;

    private Runnable runnable;

    RunnableDecorator(@NonNull AsyncContext context,
                      @NonNull FlightRecorder recorder,
                      @NonNull Runnable runnable) {
        this.context = context;
        this.recorder = recorder;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        runWithNewMdcContext((Supplier<Object>) () -> {
            try {
                long start = System.currentTimeMillis();

                runnable.run();

                recorder.getTotalRt().addAndGet((System.currentTimeMillis() - start));
                recorder.getSuccess().incrementAndGet();
            } catch (Throwable e) {
                recorder.getFailure().incrementAndGet();
                monitorLogger.error("task: '{}' exec error, thread: [{}].", taskInfo(), Thread.currentThread().getName(), e);
                throw e;
            }

            return null;
        }, context.getMdcContext());
    }

    @Override
    public void execute(AsyncContext context) {
    }

    @Override
    public String taskInfo() {
        return runnable.getClass().getName();
    }
}

class CallableDecorator implements AsyncCallable {

    private AsyncContext context;

    private FlightRecorder recorder;

    private Callable callable;

    CallableDecorator(@NonNull AsyncContext context,
                      @NonNull FlightRecorder recorder,
                      @NonNull Callable callable) {
        this.context = context;
        this.recorder = recorder;
        this.callable = callable;
    }

    @Override
    public Object call() throws Exception {
        return runWithNewMdcContext((ExceptionableSupplier<Object>) () -> {
            try {
                long start = System.currentTimeMillis();

                Object result = callable.call();

                recorder.getTotalRt().addAndGet((System.currentTimeMillis() - start));
                recorder.getSuccess().incrementAndGet();

                return result;
            } catch (Throwable e) {
                recorder.getFailure().incrementAndGet();
                monitorLogger.error("task: '{}' exec error, thread: [{}].", taskInfo(), Thread.currentThread().getName(), e);
                throw e;
            }
        }, context.getMdcContext());
    }

    @Override
    public String taskInfo() {
        return callable.getClass().getName();
    }

    @Override
    public Object execute(AsyncContext context) {
        return null;
    }
}

class AsyncRunnableDecorator implements AsyncRunnable {

    private AsyncContext context;

    private FlightRecorder recorder;

    private AsyncRunnable asyncRunnable;

    AsyncRunnableDecorator(@NonNull AsyncContext context,
                           @NonNull FlightRecorder recorder,
                           @NonNull AsyncRunnable asyncRunnable) {
        this.context = context;
        this.recorder = recorder;
        this.asyncRunnable = asyncRunnable;
    }

    @Override
    public void run() {
        runWithNewMdcContext((Supplier<Object>) () -> {
            try {
                long start = System.currentTimeMillis();

                asyncRunnable.execute(context);

                recorder.getTotalRt().addAndGet((System.currentTimeMillis() - start));
                recorder.getSuccess().incrementAndGet();
            } catch (Throwable e) {
                recorder.getFailure().incrementAndGet();
                monitorLogger.error("task: '{}' exec error, thread: [{}].", taskInfo(), Thread.currentThread().getName(), e);
                throw e;
            }

            return null;
        }, context.getMdcContext());
    }

    @Override
    public String taskInfo() {
        return asyncRunnable.taskInfo();
    }

    @Override
    public void execute(AsyncContext context) {
    }
}

/**
 * 包装成为AsyncXX, 方便ExecutorMonitor调用.
 */
class AsyncCallableDecorator implements AsyncCallable {

    private AsyncContext context;

    private FlightRecorder recorder;

    private AsyncCallable asyncCallable;


    AsyncCallableDecorator(@NonNull AsyncContext context,
                           @NonNull FlightRecorder recorder,
                           @NonNull AsyncCallable asyncCallable) {
        this.context = context;
        this.recorder = recorder;
        this.asyncCallable = asyncCallable;
    }

    @Override
    public Object call() throws Exception {
        return runWithNewMdcContext((ExceptionableSupplier<Object>) () -> {
            try {
                long start = System.currentTimeMillis();

                Object result = asyncCallable.execute(context);

                recorder.getTotalRt().addAndGet((System.currentTimeMillis() - start));
                recorder.getSuccess().incrementAndGet();

                return result;
            } catch (Throwable e) {
                recorder.getFailure().incrementAndGet();
                monitorLogger.error("task: '{}' exec error, thread: [{}].", taskInfo(), Thread.currentThread().getName(), e);
                throw e;
            }
        }, context.getMdcContext());
    }

    @Override
    public String taskInfo() {
        return asyncCallable.taskInfo();
    }

    @Override
    public Object execute(AsyncContext context) {
        return null;
    }

}