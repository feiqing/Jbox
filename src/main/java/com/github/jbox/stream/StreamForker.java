package com.github.jbox.stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.1
 * @since 2017/6/26 17:28:00.
 */
public class StreamForker<T> {

    private static final Object END_OF_STREAM = new Object();

    private final Stream<T> stream;

    /**
     * Function<Stream<T>, ?> 看似是在消费'Stream<T>', 实际上是在消费'BlockingQ<T>'内的数据
     */
    private final Map<Object, Function<Stream<T>, ?>> forks = new HashMap<>();

    public StreamForker(Stream<T> stream) {
        this.stream = stream;
    }

    public StreamForker<T> fork(Object key, Function<Stream<T>, ?> function) {
        forks.put(key, function);
        return this;
    }

    public Results getResults() {
        ForkingStreamConsumer consumer = createStreamConsumer();
        try {
            stream.sequential().forEach(consumer);
        } finally {
            consumer.finish();
        }

        return consumer;
    }

    public interface Results {
        <R> R get(Object key);
    }

    private ForkingStreamConsumer createStreamConsumer() {

        // 将初始化时注入的'Stream<T>'内的数据轮询塞入各个'BlockingQ<T>'中
        List<BlockingQueue<T>> queues = new ArrayList<>(forks.size());
        Map<Object, Future<?>> actions = new HashMap<>();

        // transfer Map<Object, Function> -> Map<Object, Future>
        for (Map.Entry<Object, Function<Stream<T>, ?>> entry : forks.entrySet()) {
            BlockingQueue<T> queue = new LinkedBlockingQueue<>();
            queues.add(queue);

            Stream<T> source = StreamSupport.stream(new BlockingQueueSpliterator(queue), false);
            // 启动异步线程, 开始tack BlockingQ里面的数据, 但由于现在Q里面尚未数据, 所以线程阻塞
            Future<?> future = CompletableFuture.supplyAsync(() -> entry.getValue().apply(source));

            actions.put(entry.getKey(), future);
        }

        return new ForkingStreamConsumer(queues, actions);
    }

    @SuppressWarnings("unchecked")
    private class ForkingStreamConsumer implements Consumer<T>, Results {

        private final List<BlockingQueue<T>> queues;

        private final Map<Object, Future<?>> actions;

        public ForkingStreamConsumer(List<BlockingQueue<T>> queues, Map<Object, Future<?>> actions) {
            this.queues = queues;
            this.actions = actions;
        }

        @Override
        public void accept(T t) {
            // 将原始'Stream<T>'内的数据轮询push到每个任务的'BlockingQ'内
            for (BlockingQueue<T> queue : queues) {
                queue.offer(t);
            }
        }

        public void finish() {
            // 给每个'BlockingQ'塞入一条代表消费结束的消息
            for (BlockingQueue<T> queue : queues) {
                queue.offer((T) END_OF_STREAM);
            }
        }

        @Override
        public <R> R get(Object key) {
            try {
                return ((Future<R>) actions.get(key)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new StreamForkerException(e);
            }
        }
    }

    private class BlockingQueueSpliterator implements Spliterator<T> {

        private BlockingQueue<T> queue;

        public BlockingQueueSpliterator(BlockingQueue<T> queue) {
            this.queue = queue;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            /**
             * tips:
             *   1. 当queue内没数据时, 需要让线程等待
             *   2. 当线程消费完数据时, 又需要让线程停止运行
             */
            while (true) {
                try {
                    T item = queue.take();
                    if (item != END_OF_STREAM) {
                        consumer.accept(item);
                        return true;
                    } else {
                        return false;
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return 0;
        }

        @Override
        public int characteristics() {
            return 0;
        }
    }
}
