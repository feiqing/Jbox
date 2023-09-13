package com.alibaba.jbox.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author jifang
 * @since 2016/11/17 下午5:31.
 */
public class DateUtilTest {

    /**
     * 6个线程 跑10万次 timeFormat timeParse 查看耗时 & 正确性
     */
    @Test
    public void testDataUtils() {

        long start = System.currentTimeMillis();
        final String[] data = {
                "1999-11-01 11:11:11",
                "2001-07-08 06:06:06",
                "2007-01-31 02:28:31",
                "1999-11-01 11:11:11",
                "2001-07-08 06:06:06",
                "2007-01-31 02:28:31"};

        List<Thread> threads = new ArrayList<>(data.length);
        AtomicLong success = new AtomicLong(0L);
        AtomicLong failed = new AtomicLong(0L);
        for (int i = 0; i < data.length; ++i) {
            final int i2 = i;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100_000; j++) {
                        String from = data[i2];
                        Date d = T.timeParse(from);
                        String to = T.timeFormat(d.getTime());
                        System.out.println("i: " + i2 + "\tj: " + j + "\tThreadID: "
                                + Thread.currentThread().getId() + "\tThreadName: "
                                + Thread.currentThread().getName() + "\t" + from + "\t" + to);
                        if (!from.equals(to)) {
                            failed.incrementAndGet();
                        } else {
                            success.incrementAndGet();
                        }
                    }
                }
            }, "formatter_test_thread_" + i);

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("total consume [" + (System.currentTimeMillis() - start) / 1000 + "]s, success:" + success + ", failed:" + failed + ".");
    }
}