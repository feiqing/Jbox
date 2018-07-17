package com.github.jbox.executor.policy;

import com.github.jbox.executor.AsyncRunnable;
import com.github.jbox.executor.ExecutorLoggerInner;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author jifang
 * @since 2017/1/16 下午2:18.
 */
public class CallerRunsPolicy extends ThreadPoolExecutor.CallerRunsPolicy implements ExecutorLoggerInner {

    public static final RejectedExecutionHandler instance = new CallerRunsPolicy(DEFAULT_GROUP);

    private String group;

    public CallerRunsPolicy(String group) {
        this.group = group;
    }

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {

        if (runnable instanceof AsyncRunnable) {
            AsyncRunnable asyncRunnable = (AsyncRunnable) runnable;
            String message = generatePolicyLoggerContent(group, this, executor.getQueue(), asyncRunnable.taskInfo(),
                    Objects.hashCode(asyncRunnable));

            monitorLogger.warn(message);
            executorLogger.warn(message);
        }

        super.rejectedExecution(runnable, executor);
    }
}
