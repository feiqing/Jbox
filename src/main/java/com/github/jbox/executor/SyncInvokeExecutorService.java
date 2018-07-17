package com.github.jbox.executor;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/9/22 14:04:00.
 */
public class SyncInvokeExecutorService extends AbstractExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(SyncInvokeExecutorService.class);

    @Override
    public void shutdown() {
        logger.info("shutdown.");
    }

    @Override
    public List<Runnable> shutdownNow() {
        logger.info("shutdownNow.");
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return true;
    }

    @Override
    public boolean isTerminated() {
        return true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void execute(Runnable command) {
        Preconditions.checkNotNull(command, "command can not be null.");
        command.run();
    }
}
