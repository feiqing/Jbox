package com.github.jbox.executor;

/**
 * 类似{@code java.lang.Runnable}结构, 在{@code Runnable}基础上添加:
 * -  {@code taskInfo()}
 * 等方法, 相比于原生的{@code Runnable}有以下优势:
 * 1.  在task执行前设置rpc context信息, 以及将traceId塞入MDC;
 * 2.  监控task执行耗时(rt);
 * 3.  监控task执行次数(success、failure), 以及据此计算得出的tps(详见{@code ExecutorMonitor};
 * 4.  监控task执行出错时打印详细log;
 * 5.  当task进入RunnableQueue后/触发{@code java.util.concurrent.RejectedExecutionHandler}时打印更详细的的信息.
 * 6.  持续完善中...
 * <p>
 * (使用了{@code ExecutorManager}后, 即使'submit()'使用的是原生Runnable, 也会被封装成一个AsyncRunnable)
 *
 * @author jifang
 * @version 1.1
 * @since 2016/12/20 上午11:09.
 */
@FunctionalInterface
public interface AsyncRunnable extends java.lang.Runnable, ExecutorLoggerInner {

    /**
     * implements like {@code Runnable.run()}
     */
    void execute(AsyncContext context);

    /**
     * detail info of the task need to invoke.
     *
     * @return default task class name.
     */
    default String taskInfo() {
        return this.getClass().getName();
    }

    /**
     * default implements Runnable, you should not change this implementation !!!
     */
    @Override
    default void run() {
        execute(null);
    }
}
