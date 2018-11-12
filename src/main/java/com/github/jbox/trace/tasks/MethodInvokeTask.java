package com.github.jbox.trace.tasks;

import com.github.jbox.job.JobTask;
import com.github.jbox.trace.TraceJobContext;
import lombok.Data;

/**
 * 方法的最终执行器, 可以覆盖该类并配置自己的执行器.
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-26 08:18:00.
 */
@Data
public class MethodInvokeTask implements JobTask<TraceJobContext> {

    private static final long serialVersionUID = 2587891236640265365L;

    @Override
    public void invoke(TraceJobContext context) throws Throwable {
        context.successOf(context.getJoinPoint().proceed(context.getArgs()));
    }
}
