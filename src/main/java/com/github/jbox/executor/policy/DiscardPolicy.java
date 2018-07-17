package com.github.jbox.executor.policy;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import com.github.jbox.executor.AsyncRunnable;
import com.github.jbox.executor.ExecutorLoggerInner;

/**
 * @author jifang
 * @since 2017/1/18 下午4:12.
 */
public class DiscardPolicy extends ThreadPoolExecutor.DiscardPolicy implements ExecutorLoggerInner {

    public static final RejectedExecutionHandler instance = new DiscardPolicy(DEFAULT_GROUP);

    private String group;

    public DiscardPolicy(String group) {
        this.group = group;
    }

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        if (runnable instanceof AsyncRunnable) {
            AsyncRunnable asyncRunnable = (AsyncRunnable)runnable;
            String message = generatePolicyLoggerContent(group, this, executor.getQueue(), asyncRunnable.taskInfo(),
                Objects.hashCode(asyncRunnable));

            monitorLogger.warn(message);
            executorLogger.warn(message);
        }

        // 直接将新元素扔掉
        super.rejectedExecution(runnable, executor);
    }
}
