package com.github.jbox.oplog;

import com.lmax.disruptor.WorkHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/22 9:24 PM.
 */
@Slf4j
@AllArgsConstructor
class OplogConsumer implements WorkHandler<Oplog> {

    private OplogHandler handler;

    @Override
    public void onEvent(Oplog event) {
        try {
            handler.handle(event);
        } catch (Throwable t) {
            log.error("handle event:{} error.", event, t);
        }
    }
}
