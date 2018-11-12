package com.github.jbox.job;

import com.github.jbox.utils.Collections3;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-09-28 20:58:00.
 */
@Data
@Slf4j(topic = "JobFramework")
public class JobContext implements Serializable {

    private static final long serialVersionUID = 6258054189501546389L;

    // 返回值相关
    private boolean success = true;

    private Object result;

    private Throwable t;

    // 框架执行需要的Meta信息, 千万不要动
    private Meta meta = new Meta();

    public JobContext() {
    }

    public JobContext(String jobName, List<? extends JobTask> jobTasks) {
        meta.jobName = jobName;
        meta.jobTasks = jobTasks;
    }

    public void successOf(Object result) {
        this.success = true;
        this.result = result;
    }

    public void errorOf(Object result) {
        this.errorOf(result, null);
    }

    public void errorOf(Object result, Throwable t) {
        this.success = false;
        this.result = result;
        this.t = t;
    }

    public void next() throws Throwable {
        try {
            meta.executingIndex++;
            if (meta.executingIndex <= meta.executedIndex) {
                throw new IllegalStateException("task has already been invoked.");
            }

            meta.executedIndex++;
            if (meta.executingIndex < meta.jobTasks.size()) {
                JobTask task = meta.jobTasks.get(meta.executingIndex);
                String desc = desc(task);
                try {
                    if (meta.traceEntry) logTrace("entry {} ...", desc);

                    meta.setAttribute(desc, System.currentTimeMillis());
                    task.invoke(this);
                } finally {
                    meta.totalCost = System.currentTimeMillis() - (long) meta.removeAttribute(desc);
                    long cost = meta.totalCost - meta.tasksCost;
                    meta.tasksCost += cost;

                    if (meta.traceExit) logTrace("exit  {}, cost:[{}]", desc, cost);
                }
                if (meta.executedIndex < meta.jobTasks.size() && meta.executedIndex == meta.executingIndex) {
                    if (meta.traceInterrupt) logTrace("[{}] execution was interrupted by {}", meta.jobName, desc);
                }
            } else {
                if (meta.traceEnd) logTrace("[{}] reaches its end.", meta.jobName);
            }
        } finally {
            meta.executingIndex--;
        }
    }

    private void logTrace(String template, Object... args) {
        if (log.isTraceEnabled()) {
            log.trace(template, args);
        }
    }

    private String desc(JobTask jobTask) {
        return MessageFormatter.arrayFormat("{}[{}/{}:{}]",
                new Object[]{
                        meta.jobName,
                        (meta.executingIndex + 1),
                        meta.jobTasks.size(),
                        jobTask.desc()
                }).getMessage();
    }

    /**
     * 你re release就行了...
     * 将复杂对象置为空, GC加速
     * FBI Warning: 在Node执行的过程中间千万不要执行该方法
     */
    public void release() {
        if (Collections3.isNotEmpty(meta.attributes)) {
            meta.attributes.clear();
        }

        result = null;
    }

    @Data
    public static class Meta implements Serializable {

        private static final long serialVersionUID = -6456235387227434364L;

        private String jobName;

        private List<? extends JobTask> jobTasks;

        private int executedIndex = -1;

        private int executingIndex = -1;

        private long tasksCost = 0;

        private long totalCost = 0;

        private boolean traceEntry = true;

        private boolean traceExit = true;

        private boolean traceEnd = true;

        private boolean traceInterrupt = true;

        // 暂时还不知道名字的属性, 先写到Attribute里面, 如果常用的话, 可以单独再起一个字段
        private Map<String, Object> attributes;

        Map<String, Object> getAttributes() {
            if (this.attributes == null) {
                this.attributes = new HashMap<>();
            }
            return this.attributes;
        }

        Object removeAttribute(String key) {
            if (attributes == null) {
                return null;
            }

            return attributes.remove(key);
        }

        void setAttribute(String key, Object value) {
            getAttributes().put(key, value);
        }
    }
}