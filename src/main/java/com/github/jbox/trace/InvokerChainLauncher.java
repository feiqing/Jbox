package com.github.jbox.trace;

import com.github.jbox.trace.nodes.MethodInvokeInvokerNode;
import com.google.common.base.Preconditions;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-26 08:25:00.
 */
class InvokerChainLauncher {

    private static final MethodInvokeInvokerNode DEFAULT_INVOKER = new MethodInvokeInvokerNode();

    private final NodeContext context;

    private InvokerChainLauncher(NodeContext context) {
        this.context = context;
    }

    public static InvokerChainLauncher newLauncher(List<InvokerNode> invokerChain,
                                                   ProceedingJoinPoint joinPoint, Class<?> clazz, Method method,
                                                   Object target, Object[] args) {
        if (invokerChain == null) {
            invokerChain = new ArrayList<>();
        }

        boolean isFoundMethodInvoker = false;
        for (int i = 0; i < invokerChain.size(); ++i) {
            InvokerNode node = invokerChain.get(i);
            Preconditions.checkNotNull(node, "node[" + i + "] is null.");

            if (node instanceof MethodInvokeInvokerNode) {
                isFoundMethodInvoker = true;
            }
        }

        if (!isFoundMethodInvoker) {
            invokerChain.add(DEFAULT_INVOKER);
        }

        if (!(invokerChain instanceof ArrayList)) {
            invokerChain = new ArrayList<>(invokerChain);
        }
        
        NodeContext context = new NodeContext(invokerChain);
        context.setJoinPoint(joinPoint);
        context.setClazz(clazz);
        context.setMethod(method);
        context.setTarget(target);
        context.setArgs(args);

        return new InvokerChainLauncher(context);
    }

    public InvokerChainLauncher emit() throws Throwable {
        context.next();
        return this;
    }

    public Object getResult() {
        return context.getResult();
    }
}
