package com.github.jbox.executor;

import com.github.jbox.executor.policy.DiscardOldestPolicy;
import com.github.jbox.utils.Collections3;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-22 02:57:00.
 */
@Slf4j
public class AsyncExecutor<T> {

    private static final long timeout = 10 * 1000;

    private static final String group = "ParallelsJobWorker";

    private static final RejectedExecutionHandler handler = new DiscardOldestPolicy(group);

    private static final ExecutorService defaultWorker = ExecutorManager.newFixedMinMaxThreadPool(group, 1, 10, 1024, handler);

    private ExecutorService worker;

    @Getter
    private List<Supplier<T>> tasks;

    @Getter
    private List<Future<T>> futures;

    private List<T> results;

    public AsyncExecutor() {
        this(defaultWorker);
    }

    public AsyncExecutor(ExecutorService worker) {
        Preconditions.checkNotNull(worker);
        this.worker = worker;
        this.tasks = new LinkedList<>();
    }

    public AsyncExecutor<T> addTask(Supplier<T> task) {
        this.tasks.add(task);
        return this;
    }

    public AsyncExecutor<T> addTasks(List<Supplier<T>> tasks) {
        this.tasks.addAll(tasks);
        return this;
    }

    public AsyncExecutor<T> execute() {
        if (Collections3.isEmpty(tasks)) {
            return this;
        }

        this.futures = new ArrayList<>(tasks.size());
        for (Supplier<T> task : tasks) {
            Future<T> future = worker.submit((AsyncCallable<T>) context -> task.get());
            futures.add(future);
        }

        return this;
    }

    public AsyncExecutor<T> waiting() {
        return waiting(timeout, true);
    }

    public AsyncExecutor<T> waiting(long millisTimeout, boolean fullyCompletes) {
        if (Collections3.isEmpty(futures)) {
            return this;
        }

        if (fullyCompletes) {
            waitingAsFullyComplete(millisTimeout);
        } else {
            waitingAsPartlyComplete(millisTimeout);
        }

        return this;
    }

    private void waitingAsFullyComplete(long millisTimeout) {
        long start = System.currentTimeMillis();
        Collections.shuffle(futures);
        int waitingSize = futures.size();
        this.results = new ArrayList<>(waitingSize);

        while (results.size() != waitingSize) {
            if (isTimeout(start, millisTimeout)) {
                this.results = Collections.emptyList();
                log.warn("waiting timeout.");
                return;
            }

            doWaiting(futures, results);
        }
    }


    private void waitingAsPartlyComplete(long millisTimeout) {
        long start = System.currentTimeMillis();
        Collections.shuffle(futures);
        int waitingSize = futures.size();
        this.results = new ArrayList<>(waitingSize);

        while (results.size() != waitingSize) {
            if (isTimeout(start, millisTimeout)) {
                log.warn("waiting timeout, partly return.");
                return;
            }

            doWaiting(futures, results);
        }
    }

    private void doWaiting(List<Future<T>> futures, List<T> results) {
        Iterator<Future<T>> futureIter = futures.iterator();
        while (futureIter.hasNext()) {
            Future<T> future = futureIter.next();
            if (future.isDone()) {
                T result = Futures.getUnchecked(future);
                results.add(result);
                futureIter.remove();
            } else if (future.isCancelled()) {
                futureIter.remove();
            }
        }
    }

    private boolean isTimeout(long start, long limit) {
        return System.currentTimeMillis() - start > limit;

    }

    public List<T> getResults() {
        return Collections3.nullToEmpty(this.results);
    }

    public List<T> getResults(Predicate<T> filter) {
        // todo: 后面可以做些优化, 在每个task并发返回前做这个工作
        return getResults().stream().filter(filter).collect(toList());
    }

    public <R> List<R> getResults(Function<T, R> map) {
        // todo : 同上
        return getResults().stream().map(map).collect(toList());
    }
}
