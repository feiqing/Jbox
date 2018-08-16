package com.github.jbox.executor;

import com.github.jbox.executor.policy.DiscardOldestPolicy;
import com.github.jbox.utils.Collections3;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.github.jbox.executor.AsyncJobExecutor.JobStatus.*;
import static java.util.stream.Collectors.toList;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-22 02:57:00.
 */
@Slf4j
public class AsyncJobExecutor<T> {

    private static final long timeout = 10 * 1000;

    private static final String group = "ParallelsJobWorker";

    private static final RejectedExecutionHandler handler = new DiscardOldestPolicy(group);

    private static final ExecutorService defaultWorker = ExecutorManager.newFixedMinMaxThreadPool(group, 5, 10, 1024, handler);

    private ExecutorService worker;

    @Getter
    private List<Supplier<T>> tasks;

    @Getter
    private int remain;

    @Getter
    private List<Future<T>> futures;

    private List<T> results;

    private CountDownLatch latch;

    public AsyncJobExecutor() {
        this(defaultWorker);
    }

    public AsyncJobExecutor(ExecutorService worker) {
        Preconditions.checkNotNull(worker);
        this.worker = worker;
        this.tasks = new LinkedList<>();
    }

    public AsyncJobExecutor<T> addTask(Supplier<T> task) {
        this.tasks.add(task);
        return this;
    }

    public AsyncJobExecutor<T> addTasks(List<Supplier<T>> tasks) {
        this.tasks.addAll(tasks);
        return this;
    }

    public AsyncJobExecutor<T> execute() {
        if (Collections3.isEmpty(tasks)) {
            return this;
        }

        this.latch = new CountDownLatch(tasks.size());
        this.futures = new ArrayList<>(tasks.size());
        for (Supplier<T> task : tasks) {
            Future<T> future = worker.submit((AsyncCallable<T>) context -> {

                T result = task.get();
                latch.countDown();

                return result;
            });

            futures.add(future);
        }

        return this;
    }

    public AsyncJobExecutor<T> waiting() {
        return waiting(timeout, true);
    }

    public AsyncJobExecutor<T> waiting(long millisTimeout, boolean fullyCompletes) {
        if (Collections3.isEmpty(futures)) {
            return this;
        }

        try {
            latch.await(millisTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("task await error.", e);
        }

        this.remain = (int) latch.getCount();
        this.results = new ArrayList<>(futures.size() - remain);
        if (remain == 0 || !fullyCompletes) {
            this.doGetResult(this.futures, this.results);
        }

        return this;
    }

    private void doGetResult(List<Future<T>> futures, List<T> results) {
        for (Future<T> future : futures) {
            if (future.isDone()) {
                T result = Futures.getUnchecked(future);
                results.add(result);
            }
        }
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

    public JobStatus getJobStatus() {
        if (Collections3.isEmpty(futures)) {
            return INIT;
        }

        boolean allDone = true;
        for (Future future : futures) {
            if (!future.isDone() || !future.isCancelled()) {
                return DOING;
            }

            if (future.isCancelled()) {
                allDone = false;
            }
        }
        return allDone ? DONE : HALF_DONE;
    }

    public enum JobStatus {
        INIT, DOING, HALF_DONE, DONE
    }
}
