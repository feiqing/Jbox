package com.github.jbox.scheduler;

import com.github.jbox.executor.AsyncContext;
import com.github.jbox.executor.AsyncRunnable;
import com.github.jbox.executor.ExecutorManager;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.jbox.scheduler.ScheduleTask.SCHEDULE_TASK_LOGGER;

/**
 * @author jifang
 * @version v1.2
 * @since 16/8/23 下午3:35.
 */
public class TaskScheduler implements ApplicationContextAware, DisposableBean {

    /**
     * 基本调度时间片
     */
    private static final int BASE_TIME_FRAGMENT = 100;

    private static final String group = "com.github.jbox.scheduler:TaskScheduler";

    private static final ScheduledExecutorService scheduleExecutor = ExecutorManager.newSingleThreadScheduledExecutor(group);

    private static final List<TaskRegister> taskRegisters = new ArrayList<>();

    private static final List<ScheduleTask> tasks = new ArrayList<>();

    public static void registerExternalTask(ScheduleTask task) {
        tasks.add(task);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Collection<ScheduleTask> scheduleTasks = getNeedRegisterTask(applicationContext);
        List<ScheduleTask> needInvokeOnStart = new LinkedList<>();
        for (ScheduleTask scheduleTask : scheduleTasks) {
            taskRegisters.add(TaskRegister.newRegister(scheduleTask));
            if (scheduleTask.configuration().isInvokeAtStart()) {
                needInvokeOnStart.add(scheduleTask);
            }

            SCHEDULE_TASK_LOGGER.info("scheduleTask [{}] registered, period [{}]", scheduleTask.taskDesc(), scheduleTask.configuration().getPeriod());
        }

        // invoke start task
        needInvokeOnStart.forEach(this::invokeTask);

        scheduleExecutor.scheduleAtFixedRate(
                this::triggerTask,
                BASE_TIME_FRAGMENT,
                BASE_TIME_FRAGMENT,
                TimeUnit.MILLISECONDS
        );
        SCHEDULE_TASK_LOGGER.info("TaskScheduler Start ...");
    }

    private void triggerTask() {
        taskRegisters.forEach(taskRegister -> {

            long currentFragment = taskRegister.incrCurrentFragment();
            long triggerFragment = taskRegister.getTriggerFragment();

            if (currentFragment >= triggerFragment) {
                // 执行任务
                invokeTask(taskRegister.getScheduleTask());
                // 重新开始计时
                taskRegister.clearCurrentFragment();
            }
        });
    }

    private void invokeTask(ScheduleTask task) {
        task.configuration().getExecutor().submit(new AsyncRunnable() {
            @Override
            public void execute(AsyncContext context) {
                try {
                    task.schedule();
                    SCHEDULE_TASK_LOGGER.debug("task [{}] invoked", task.taskDesc());
                } catch (Throwable e) {
                    SCHEDULE_TASK_LOGGER.error("task [{}] invoke error", task.taskDesc(), e);
                }
            }

            @Override
            public String taskInfo() {
                return task.taskDesc();
            }
        });
    }

    private List<ScheduleTask> getNeedRegisterTask(ApplicationContext applicationContext) {
        Map<String, ScheduleTask> beans = applicationContext.getBeansOfType(ScheduleTask.class);
        if (CollectionUtils.isEmpty(beans)) {
            return tasks;
        }

        Collection<ScheduleTask> scheduleTasks = beans.values();
        for (ScheduleTask task : scheduleTasks) {
            if (task.configuration().isAutoRegistered()) {
                tasks.add(task);
            }
        }

        return tasks;
    }

    @Override
    public void destroy() {
        scheduleExecutor.shutdown();
        SCHEDULE_TASK_LOGGER.info("TaskScheduler shutdown ...");
    }

    @Data
    private static class TaskRegister {

        private ScheduleTask scheduleTask;

        private long currentFragment;

        private long triggerFragment;

        long incrCurrentFragment() {
            this.currentFragment = this.currentFragment + 1;
            return this.currentFragment;
        }

        void clearCurrentFragment() {
            this.currentFragment = 0L;
        }

        static TaskRegister newRegister(ScheduleTask scheduleTask) {
            if (scheduleTask.configuration().getPeriod() < BASE_TIME_FRAGMENT) {
                throw new RuntimeException("ScheduleTask: " + scheduleTask.taskDesc() + "'s period is less-than BASE_TIME_FRAGMENT[100ms].");
            }

            TaskRegister taskRegister = new TaskRegister();
            taskRegister.setScheduleTask(scheduleTask);
            taskRegister.setCurrentFragment(0L);
            taskRegister.setTriggerFragment(scheduleTask.configuration().getPeriod() / BASE_TIME_FRAGMENT);

            return taskRegister;
        }
    }
}
