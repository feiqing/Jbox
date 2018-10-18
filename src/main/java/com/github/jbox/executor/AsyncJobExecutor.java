package com.github.jbox.executor;

import com.github.jbox.executor.policy.DiscardOldestPolicy;
import com.github.jbox.utils.Collections3;
import com.github.jbox.utils.Objects2;
import com.google.common.base.Strings;
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

    private static final long defaultTimeout = 10 * 1000;

    private static final String defaultGroup = "AsyncJobExecutor";

    private ExecutorService worker;

    @Getter
    private List<Supplier<T>> tasks;

    @Getter
    private int remain;

    @Getter
    private List<Future<T>> futures;

    private boolean hasRunningException;

    private String jobDesc;

    private List<T> results;

    private CountDownLatch latch;

    public AsyncJobExecutor() {
        this(null);
    }

    public AsyncJobExecutor(ExecutorService worker) {
        this.worker = Objects2.nullToDefault(worker, getWorker());
        this.tasks = new LinkedList<>();
    }

    protected ExecutorService getWorker() {
        RejectedExecutionHandler handler = new DiscardOldestPolicy(defaultGroup);
        return ExecutorManager.newFixedMinMaxThreadPool(defaultGroup, 5, 10, 1024, handler);
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
            Future<T> future = worker.submit(
                    new AsyncCallable<T>() {

                        @Override
                        public T execute(AsyncContext context) {
                            try {
                                return task.get();
                            } catch (Throwable t) {
                                hasRunningException = true;
                                throw t;
                            } finally {
                                latch.countDown();
                            }
                        }

                        @Override
                        public String taskInfo() {
                            String jobDesc = getJobDesc();
                            return Strings.isNullOrEmpty(jobDesc) ? this.getClass().getName() : jobDesc;
                        }
                    }
            );

            futures.add(future);
        }

        return this;
    }

    public AsyncJobExecutor<T> waiting() {
        return waiting(defaultTimeout, true);
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

    public AsyncJobExecutor<T> appendJobDesc(String jobDesc) {
        if (this.jobDesc == null) {
            this.jobDesc = jobDesc;
        } else {
            this.jobDesc = this.jobDesc + ',' + jobDesc;
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

        if (hasRunningException) {
            return BROKEN;
        }

        boolean allDone = true;
        for (Future future : futures) {
            if (!future.isDone() && !future.isCancelled()) {
                return DOING;
            }

            if (future.isCancelled()) {
                allDone = false;
            }
        }
        return allDone ? DONE : HALF_DONE;
    }

    public String getJobDesc() {
        return Strings.nullToEmpty(this.jobDesc);
    }

    public enum JobStatus {
        INIT, DOING, BROKEN, HALF_DONE, DONE
    }
}
