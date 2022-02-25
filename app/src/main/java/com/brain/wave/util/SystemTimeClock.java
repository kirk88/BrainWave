package com.brain.wave.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 时钟工具
 **/
public class SystemTimeClock {

    /**
     * 设置周期
     */
    private final long period;

    /**
     * 用来作为我们时间戳存储的容器
     */
    private final AtomicLong now;

    private SystemTimeClock(long period) {
        this.period = period;
        this.now = new AtomicLong(System.currentTimeMillis());
        scheduleClockUpdating();
    }

    /**
     * 初始化单例
     *
     * @return
     */
    private static SystemTimeClock instance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * 获取毫秒时间戳 替换System.currentTimeMillis()
     *
     * @return
     */
    public static long currentTimeMillis() {
        return instance().now();
    }

    private long now() {
        return now.get();
    }

    /**
     * 初始化定时器
     */
    private void scheduleClockUpdating() {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "System Clock");
            //设置为守护线程
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(() -> now.set(System.currentTimeMillis()), period, period, TimeUnit.MILLISECONDS);
    }

    private static class InstanceHolder {
        //设置1ms更新一次时间
        static final SystemTimeClock INSTANCE = new SystemTimeClock(1);
    }
}
