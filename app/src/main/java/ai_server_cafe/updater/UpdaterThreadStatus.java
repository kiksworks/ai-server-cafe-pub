package ai_server_cafe.updater;

import ai_server_cafe.util.interfaces.WrapperWeakCloneableMap;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public final class UpdaterThreadStatus {
    private static UpdaterThreadStatus instance;
    private final Map<String, Deque<Double>> map;
    private final double bufferTime = 1.0;
    private UpdaterThreadStatus() {
        this.map = Collections.synchronizedMap(new HashMap<>());
    }

    public static synchronized UpdaterThreadStatus getInstance() {
        if (instance == null) {
            instance = new UpdaterThreadStatus();
        }
        return instance;
    }

    public synchronized void addTPSData(String threadName, double lastCycleTime) {
        if (!this.map.containsKey(threadName)) {
            this.map.put(threadName, new ArrayDeque<>());
        }
        this.map.get(threadName).addLast(lastCycleTime);
        while (lastCycleTime - this.map.get(threadName).getFirst() > this.bufferTime) {
            this.map.get(threadName).pollFirst();
        }
    }

    public synchronized double getTPS(String threadName) {
        return this.map.get(threadName).size() / this.bufferTime;
    }

    @Nonnull
    public synchronized WrapperWeakCloneableMap<String, Double> getAllTPSData() {
        Map<String, Double> results = new HashMap<>();
        for (String key : this.map.keySet()) {
            results.put(key, this.map.get(key).size() / this.bufferTime);
        }
        return new WrapperWeakCloneableMap<>(results);
    }
}
