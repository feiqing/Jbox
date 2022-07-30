package com.github.jbox.trace;

import com.github.jbox.job.JobTask;
import com.github.jbox.trace.tasks.MethodInvokeTask;
import com.github.jbox.utils.Jbox;
import com.google.common.base.Preconditions;
import lombok.Setter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.CollectionUtils;

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
@Aspect
public class TraceLauncher implements Serializable {

    private static final long serialVersionUID = 1383288704716921329L;

    private List<JobTask> tasks;

    @Setter
    private boolean useAbstractMethod = false;

    public void setTasks(List<JobTask> tasks) {
        Preconditions.checkArgument(!CollectionUtils.isEmpty(tasks));

        boolean isFoundMethodInvoker = false;
        for (int i = 0; i < tasks.size(); ++i) {
            JobTask task = tasks.get(i);
            Preconditions.checkNotNull(task, "task[" + i + "] is null.");

            if (task instanceof MethodInvokeTask) {
                isFoundMethodInvoker = true;
            }
        }

        if (!isFoundMethodInvoker) {
            tasks.add(new MethodInvokeTask());
        }
        
        this.tasks = new ArrayList<>(tasks);
    }

    @Around("@annotation(com.github.jbox.trace.Trace)")
    public Object emit(final ProceedingJoinPoint joinPoint) throws Throwable {

        Method method = useAbstractMethod ? Jbox.getAbstractMethod(joinPoint) : Jbox.getImplMethod(joinPoint);
        Class<?> clazz = method.getDeclaringClass();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();

        TraceJobContext context = newContext(joinPoint, clazz, method, target, args);
        context.next();
        return context.getResult();
    }

    private TraceJobContext newContext(ProceedingJoinPoint joinPoint,
                                       Class<?> clazz, Method method,
                                       Object target, Object[] args) {

        TraceJobContext context = new TraceJobContext("TraceJob", tasks);

        context.setJoinPoint(joinPoint);
        context.setClazz(clazz);
        context.setMethod(method);
        context.setTarget(target);
        context.setArgs(args);

        return context;
    }
}
