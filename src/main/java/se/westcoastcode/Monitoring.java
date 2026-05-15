package se.westcoastcode;

import com.dslplatform.json.CompiledJson;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;

public final class Monitoring {
    private static final long STARTUP_TIME = ManagementFactory.getRuntimeMXBean().getStartTime();

    private final OperatingSystemMXBean osBean;
    private final Runtime runtime;
    private final ThreadMXBean threadMXBean;
    private final GarbageCollectorMXBean gcBean;

    public record Memory(long total, long free, long used) {
    }

    public record GC(long count, long time) {
    }

    @Data
    @AllArgsConstructor
    @CompiledJson
    public static class State {
        private long totalMemory;
        private long freeMemory;
        private long usedMemory;
        private long gcCount;
        private long gcTime;
        private double cpuUsage;
        private int threadCount;
        private long uptime;
    }

    public Monitoring() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.runtime = Runtime.getRuntime();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.gcBean = ManagementFactory.getGarbageCollectorMXBeans().getFirst();
    }

    public Memory heapMemoryUsage() {
        return new Memory(runtime.totalMemory() - runtime.freeMemory(), runtime.freeMemory(), runtime.totalMemory());
    }

    public double cpuUsage() {
        return osBean.getSystemLoadAverage();
    }

    private int threadCount() {
        return threadMXBean.getThreadCount();
    }

    private GC gc() {
        return new GC(gcBean.getCollectionCount(), gcBean.getCollectionTime());
    }

    public Duration uptime() {
        return Duration.between(Instant.ofEpochMilli(STARTUP_TIME), Instant.now());
    }

    public State current() {
        var memory = heapMemoryUsage();
        var gc = gc();
        var duration = uptime();
        return new State(memory.total, memory.free, memory.used, gc.count, gc.time, cpuUsage(), threadCount(),
                duration.toMillis());
    }
}
