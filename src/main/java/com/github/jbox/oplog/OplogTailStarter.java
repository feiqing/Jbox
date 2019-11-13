package com.github.jbox.oplog;

import com.github.jbox.utils.Collections3;
import com.google.common.base.Preconditions;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/11/12 4:35 PM.
 */
public class OplogTailStarter {

    static OplogConfig config;

    private OplogReader reader;

    public void start(OplogConfig config) {
        Preconditions.checkArgument(Collections3.isNotEmpty(config.getMongos()));
        Preconditions.checkArgument(config.getLogBatchSize() > 0);
        Preconditions.checkArgument(Collections3.isNotEmpty(config.getOps()));
        Preconditions.checkArgument(config.getHandler() != null);
        Preconditions.checkArgument(config.getRingBufferConcurrency() > 0);
        Preconditions.checkArgument(config.getRingBufferSize() > 0);

        OplogTailStarter.config = config;
        reader = new OplogReader();
        reader.start(config.getMongos());
        OplogDisruptor.start();
    }

    public void stop() {
        reader.stop();
        OplogDisruptor.stop();
    }
}
