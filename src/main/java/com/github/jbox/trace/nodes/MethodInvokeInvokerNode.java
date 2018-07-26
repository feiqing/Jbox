package com.github.jbox.trace.nodes;

import com.github.jbox.trace.NodeContext;
import com.github.jbox.trace.InvokerNode;
import lombok.Data;

/**
 * 方法的最终执行器, 可以覆盖该类并配置自己的执行器.
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-26 08:18:00.
 */
@Data
public class MethodInvokeInvokerNode implements InvokerNode {

    @Override
    public void invoke(NodeContext context) throws Throwable {
        context.setResult(context.getJoinPoint().proceed(context.getArgs()));
    }
}
