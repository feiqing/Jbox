package com.github.jbox.http;

import java.util.function.Function;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.1
 * @since 2018-05-04 22:35:00.
 */
public abstract class RespProcessor<Rsp> implements Function<RespContext, Rsp> {

    public static RespProcessor<String> identity = new RespProcessor<String>() {
        @Override
        protected String process(RespContext context) {
            return context.getResp();
        }
    };

    protected abstract Rsp process(RespContext context);

    @Override
    public Rsp apply(RespContext context) {
        // 保留可扩展的可能(如日志记录...)
        return process(context);
    }
}
