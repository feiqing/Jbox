package com.github.jbox.executor;

import com.github.jbox.utils.Collections3;
import com.github.jbox.utils.Objects2;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
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

    @Setter
    private ExecutorService worker;

    @Getter
    private List<Supplier<T>> tasks;

    @Getter
    private int remain;

    @Getter
    private List<Future<T>> futures;

    private Queue<Optional<T>> tmpResults = new ConcurrentLinkedQueue<>();

    private List<T> results;

    private volatile boolean hasRunningException;

    private String jobDesc;

    private CountDownLatch latch;

    public AsyncJobExecutor() {
        this(null);
    }

    public AsyncJobExecutor(ExecutorService worker) {
        this.worker = Objects2.nullToDefault(worker, getWorker());
        this.tasks = new LinkedList<>();
    }

    protected ExecutorService getWorker() {
        return ExecutorManager.newFixedMinMaxThreadPool("com.github.jbox.executor:AsyncJobExecutor", 20, 20, 1024, new ThreadPoolExecutor.AbortPolicy());
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
                                T result = task.get();
                                tmpResults.offer(Optional.ofNullable(result));
                                return result;
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
        return waiting(Long.MAX_VALUE, true);
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
        if (remain == 0 || !fullyCompletes) {
            this.results = new ArrayList<>(this.tmpResults.size());
            this.tmpResults.forEach(optional -> this.results.add(optional.orElse(null)));
        } else {
            this.results = new ArrayList<>();
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
