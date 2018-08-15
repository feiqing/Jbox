package com.github.jbox.executor;

import org.junit.Test;

import java.util.List;

import static org.apache.hadoop.hbase.util.Threads.sleep;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-08-15 20:50:00.
 */
public class AsyncExecutorTest {

    @Test
    public void test() {
        AsyncExecutor<Object> executor = new AsyncExecutor<>()
                .addTask(() -> {
                    sleep(80 );
                    return 1;
                }).addTask(() -> {
                    sleep(10 );
                    return 2;
                });

        List<Object> results = executor.execute().waiting(50 , false).getResults();
        System.out.println(results);
    }
}
