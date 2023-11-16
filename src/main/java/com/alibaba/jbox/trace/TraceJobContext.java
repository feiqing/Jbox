package com.alibaba.jbox.trace;

import com.alibaba.jbox.job.JobContext;
import com.alibaba.jbox.job.JobTask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018/11/11 5:07 PM.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TraceJobContext extends JobContext<String> {

    private static final long serialVersionUID = 3545382785877769347L;

    private ProceedingJoinPoint joinPoint;

    private Class<?> clazz;

    private Method method;

    private Object target;

    private Object[] args;

    public TraceJobContext() {
        super(null, null);
    }

    public TraceJobContext(String jobName, JobTask[] jobTasks) {
        super(jobName, jobTasks);
    }
}
