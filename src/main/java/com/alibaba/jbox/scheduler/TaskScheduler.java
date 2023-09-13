package com.alibaba.jbox.scheduler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author jifang
 * @version v1.2
 * @since 16/8/23 下午3:35.
 */
@Slf4j
public class TaskScheduler implements ApplicationContextAware, DisposableBean {

    private static final int BASE_TIME_FRAGMENT = 100; // 基础调度时间片

    private static final BasicThreadFactory factory = new BasicThreadFactory.Builder().namingPattern("J-Schedule-Master-Exec").daemon(true).build();

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(factory);

    private static final List<Register> register = new ArrayList<>();

    private static final List<ScheduleTask> tasks = new LinkedList<>();

    public static void registerTask(ScheduleTask task) {
        tasks.add(task);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Collection<ScheduleTask> scheduleTasks = getNeedRegisterTask(applicationContext);
        for (ScheduleTask scheduleTask : scheduleTasks) {
            register.add(Register.newRegister(scheduleTask));
            log.info("Task [{}] registered, period [{}]", scheduleTask.desc(), scheduleTask.period());
        }

        scheduler.scheduleAtFixedRate(
                this::triggerTask,
                BASE_TIME_FRAGMENT,
                BASE_TIME_FRAGMENT,
                TimeUnit.MILLISECONDS
        );
        log.info("TaskScheduler Start ...");
    }

    private void triggerTask() {
        register.forEach(taskRegister -> {

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
        task.executor().submit(() -> {
            try {
                task.schedule();
                log.debug("task [{}] invoked", task.desc());
            } catch (Throwable e) {
                log.error("task [{}] invoke error", task.desc(), e);
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
            if (task.autoRegistered()) {
                tasks.add(task);
            }
        }

        return tasks;
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
        log.info("TaskScheduler shutdown ...");
    }

    @Data
    private static class Register {

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

        static Register newRegister(ScheduleTask scheduleTask) {
            if (scheduleTask.period() < BASE_TIME_FRAGMENT) {
                throw new RuntimeException("ScheduleTask: " + scheduleTask.desc() + "'s period is less-than BASE_TIME_FRAGMENT[100ms].");
            }

            Register taskRegister = new Register();
            taskRegister.setScheduleTask(scheduleTask);
            taskRegister.setCurrentFragment(0L);
            taskRegister.setTriggerFragment(scheduleTask.period() / BASE_TIME_FRAGMENT);

            return taskRegister;
        }
    }
}
