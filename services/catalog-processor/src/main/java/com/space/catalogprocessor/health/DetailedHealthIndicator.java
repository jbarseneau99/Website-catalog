package com.space.catalogprocessor.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

@Component
public class DetailedHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        return Health.up()
                .withDetail("memory", getMemoryDetails(memoryBean))
                .withDetail("threads", getThreadDetails(threadBean))
                .withDetail("system", getSystemDetails())
                .withDetail("catalog", getCatalogDetails())
                .build();
    }

    private Object getMemoryDetails(MemoryMXBean memoryBean) {
        return Health.up()
                .withDetail("heap.used", memoryBean.getHeapMemoryUsage().getUsed())
                .withDetail("heap.max", memoryBean.getHeapMemoryUsage().getMax())
                .withDetail("nonHeap.used", memoryBean.getNonHeapMemoryUsage().getUsed())
                .withDetail("nonHeap.max", memoryBean.getNonHeapMemoryUsage().getMax())
                .build()
                .getDetails();
    }

    private Object getThreadDetails(ThreadMXBean threadBean) {
        return Health.up()
                .withDetail("live", threadBean.getThreadCount())
                .withDetail("peak", threadBean.getPeakThreadCount())
                .withDetail("daemon", threadBean.getDaemonThreadCount())
                .build()
                .getDetails();
    }

    private Object getSystemDetails() {
        Runtime runtime = Runtime.getRuntime();
        return Health.up()
                .withDetail("processors", runtime.availableProcessors())
                .withDetail("freeMemory", runtime.freeMemory())
                .withDetail("totalMemory", runtime.totalMemory())
                .withDetail("maxMemory", runtime.maxMemory())
                .build()
                .getDetails();
    }

    private Object getCatalogDetails() {
        return Health.up()
                .withDetail("processedItems", 0)
                .withDetail("queueSize", 0)
                .withDetail("lastProcessedTimestamp", System.currentTimeMillis())
                .build()
                .getDetails();
    }
} 