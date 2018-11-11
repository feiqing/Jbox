package com.github.jbox.trace;

import com.github.jbox.slot.JobSlot;
import com.github.jbox.slot.SlotContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018/11/11 5:07 PM.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TraceSlotContext extends SlotContext {

    private static final long serialVersionUID = 3545382785877769347L;

    private ProceedingJoinPoint joinPoint;

    private Class<?> clazz;

    private Method method;

    private Object target;

    private Object[] args;

    public TraceSlotContext() {
    }

    public TraceSlotContext(String jobName, List<JobSlot> jobSlots) {
        super(jobName, jobSlots);
    }
}
