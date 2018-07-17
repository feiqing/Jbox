package com.github.jbox.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/9/22 14:16:00.
 */
public class SyncExecutorTest {

    public static void main(String[] args) {
        ExecutorManager executorManager = new ExecutorManager();
        ExecutorService executorService = ExecutorManager.newFixedMinMaxThreadPool("ss", 1, 1, 1);
        executorService.submit(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                System.out.println(Thread.currentThread().getName());
                return null;
            }
        });
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName());
            }
        });

        System.out.println(Thread.currentThread().getName());
    }
}
