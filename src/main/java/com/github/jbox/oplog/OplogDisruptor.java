package com.github.jbox.oplog;

import com.github.jbox.executor.ExecutorManager;
import com.github.jbox.executor.NamedThreadFactory;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/22 9:08 PM.
 */
@Slf4j(topic = "OplogDisruptor")
class OplogDisruptor {

    private static final String GROUP = "OplogDisruptorWorker:";

    private static ConcurrentMap<String, Disruptor<Oplog>> disruptors = new ConcurrentHashMap<>();

    static Disruptor<Oplog> getDisruptor(String mongo) {
        return disruptors.computeIfAbsent(mongo, (_K) -> {
            NamedThreadFactory executors = new NamedThreadFactory(GROUP + mongo);
            int bufferSize = OplogTailStarter.config.getRingBufferSize();

            Disruptor<Oplog> disruptor = new Disruptor<>(Oplog::new, bufferSize, executors, ProducerType.SINGLE, new BlockingWaitStrategy());
            disruptor.handleEventsWithWorkerPool(getConsumers());
            disruptor.setDefaultExceptionHandler(new ExceptionHandler<Oplog>() {
                @Override
                public void handleEventException(Throwable ex, long sequence, Oplog event) {
                    log.error("", ex);
                }

                @Override
                public void handleOnStartException(Throwable ex) {
                    log.error("", ex);
                }

                @Override
                public void handleOnShutdownException(Throwable ex) {
                    log.error("", ex);
                }
            });

            disruptor.start();
            log.info("disruptor:[{}] started", mongo);

            return disruptor;
        });
    }

    private static OplogConsumer[] getConsumers() {
        int concurrency = OplogTailStarter.config.getRingBufferConcurrency();
        OplogHandler handler = OplogTailStarter.config.getHandler();

        OplogConsumer[] consumers = new OplogConsumer[concurrency];
        for (int i = 0; i < concurrency; ++i) {
            consumers[i] = new OplogConsumer(handler);
        }
        return consumers;
    }

    static void start() {
        ExecutorManager.newSingleThreadScheduledExecutor("DisruptorMonitor").scheduleAtFixedRate(() -> {
            for (Map.Entry<String, Disruptor<Oplog>> entry : disruptors.entrySet()) {
                String ns = entry.getKey();
                Disruptor<Oplog> disruptor = entry.getValue();
                log.info("disruptor [{}]: cached {}, cursor {}, remain {}, size {}.",
                        ns,
                        disruptor.getRingBuffer().getBufferSize() - disruptor.getRingBuffer().remainingCapacity(),
                        disruptor.getCursor(),
                        disruptor.getRingBuffer().remainingCapacity(),
                        disruptor.getBufferSize());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    static void stop() {
        disruptors.values().forEach(Disruptor::shutdown);
    }
}
