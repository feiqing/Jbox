package com.github.jbox.utils;

import com.github.jbox.executor.ExecutorLoggerInner;

import org.junit.Test;

/**
 * @author jifang
 * @since 2016/11/10 下午5:07.
 */
public class PerformerTest implements ExecutorLoggerInner {

    @Test
    public void testPerformer() {

        for (int i = 0; i < 1000000; ++i) {
        }

        Performer analyzer = new Performer("test");

        for (int i = 0; i < 10000; ++i) {
            analyzer.invoked();
        }

        double qps = analyzer.tps();
        System.out.println(qps);

        for (int i = 0; i < 10000; ++i) {
            analyzer.invoked();
        }
        qps = analyzer.tps();
        System.out.println(qps);

        for (int i = 0; i < 10000; ++i) {
            analyzer.invoked();
        }
        qps = analyzer.tps();
        System.out.println(qps);

        for (int i = 0; i < 10000; ++i) {
            analyzer.invoked();
        }
        qps = analyzer.tps();
        System.out.println(qps);

        for (int i = 0; i < 10000; ++i) {
            analyzer.invoked();
        }
        System.out.println(analyzer.rtString());

        for (int i = 0; i < 10000; ++i) {
            analyzer.invoked();
        }
        System.out.println(analyzer.tpsString());

    }
}
