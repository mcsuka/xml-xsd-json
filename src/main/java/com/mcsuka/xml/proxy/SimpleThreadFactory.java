package com.mcsuka.xml.proxy;


import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleThreadFactory implements ThreadFactory {

    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final int priority;
    private final boolean daemon;

    public SimpleThreadFactory(String name, int priority, boolean daemon) {
        this.priority = priority;
        this.daemon = daemon;
        group = Thread.currentThread().getThreadGroup();
        namePrefix = name + "-" + poolNumber.getAndIncrement() + "-thread-";
    }

    @Override
    public Thread newThread(@NotNull Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon() != daemon) {
            t.setDaemon(daemon);
        }

        if (t.getPriority() != priority) {
            t.setPriority(priority);
        }

        return t;
    }
}
