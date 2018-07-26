package com.github.jbox.trace;

import com.github.jbox.utils.JboxUtils;
import lombok.Data;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.8
 * - 1.0: append 'traceId' to logger;
 * - 1.1: append 'method invoke cost time & param' to biz logger;
 * - 1.2: validate method param {@code com.github.jbox.annotation.NotNull}, {@code
 * com.github.jbox.annotation.NotEmpty};
 * - 1.3: replace validator instance to hibernate-validator;
 * - 1.4: add sentinel on invoked service interface;
 * - 1.5: append method invoke result on logger content;
 * - 1.6: support use TraceLauncher not with {@code com.github.jbox.trace.TraceLauncher} annotation.
 * - 1.7: async/sync append |invokeTime|thread|rt|class|method|args|result|exception|serverIp|traceId
 * |clientName|clientIp| to TLog.
 * - 1.8: add 'errorRoot' config to determine append root logger error content.
 * @since 2016/11/25 上午11:53.
 */
@Data
@Aspect
public class TraceLauncher implements Serializable {

    private static final long serialVersionUID = 1383288704716921329L;

    private List<InvokerNode> invokerChain;

    private boolean useAbstractMethod = false;

    @Around("@annotation(com.github.jbox.trace.Trace)")
    public Object emit(final ProceedingJoinPoint joinPoint) throws Throwable {

        Method method = useAbstractMethod ? JboxUtils.getAbstractMethod(joinPoint) : JboxUtils.getImplMethod(joinPoint);
        Class<?> clazz = method.getDeclaringClass();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();

        return InvokerChainLauncher
                .newLauncher(
                        invokerChain,
                        joinPoint, clazz, method,
                        target, args
                )
                .emit()
                .getResult();
    }
}
