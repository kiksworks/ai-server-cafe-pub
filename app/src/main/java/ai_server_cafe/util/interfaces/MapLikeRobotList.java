package ai_server_cafe.util.interfaces;

import ai_server_cafe.util.TeamColor;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MapLikeRobotList<T> {
    private final Map<Integer, T> mapYellow;
    private final Map<Integer, T> mapBlue;

    public MapLikeRobotList() {
        this.mapBlue = new ConcurrentHashMap<>();
        this.mapYellow = new ConcurrentHashMap<>();
    }

    public MapLikeRobotList(@Nonnull Map<Integer, T> mapBlue, @Nonnull Map<Integer, T> mapYellow) {
        this.mapBlue = new ConcurrentHashMap<>(mapBlue);
        this.mapYellow = new ConcurrentHashMap<>(mapYellow);
    }

    public int size(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            return this.mapYellow.size();
        }
        return this.mapBlue.size();
    }

    public boolean isEmpty(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            return this.mapYellow.isEmpty();
        }
        return this.mapBlue.isEmpty();
    }

    public boolean containsKey(@Nonnull TeamColor color, int id) {
        if (color.isYellow()) {
            return this.mapYellow.containsKey(id);
        }
        return this.mapBlue.containsKey(id);
    }

    public boolean containsValue(@Nonnull T value) {
        return this.mapBlue.containsKey(value) || this.mapYellow.containsKey(value);
    }

    public boolean containsValue(@Nonnull TeamColor color, T value) {
        if (color.isYellow()) {
             return this.mapYellow.containsKey(value);
        }
        return this.mapBlue.containsKey(value);
    }

    public T get(@Nonnull TeamColor color, int id) {
        if (color.isYellow()) {
            return this.mapYellow.get(id);
        }
        return this.mapBlue.get(id);
    }

    public T put(@Nonnull TeamColor color, int id, T value) {
        if (color.isYellow()) {
            return this.mapYellow.put(id, value);
        }
        return this.mapBlue.put(id, value);
    }

    public T remove(@Nonnull TeamColor color, int id) {
        if (color.isYellow()) {
            return this.mapYellow.remove(id);
        }
        return this.mapBlue.remove(id);
    }

    public void putAll(@Nonnull Map<? extends Pair<TeamColor, Integer>, ? extends T> m) {
        for (Map.Entry<? extends Pair<TeamColor, Integer>, ? extends T> entry : m.entrySet()) {
            this.put(entry.getKey().getKey(), entry.getKey().getValue(), entry.getValue());
        }
    }

    public void putAll(@Nonnull TeamColor color, Map<Integer, ? extends T> m) {
        if (color.isYellow()) {
            this.mapYellow.putAll(m);
        } else {
            this.mapBlue.putAll(m);
        }
    }

    public void putAll(@Nonnull MapLikeRobotList<T> map) {
        for (Map.Entry<Pair<TeamColor, Integer>, T> entry : map.entrySet()) {
            if (entry.getKey().getKey().isYellow()) {
                this.mapYellow.put(entry.getKey().getValue(), entry.getValue());
            } else {
                this.mapBlue.put(entry.getKey().getValue(), entry.getValue());
            }
        }
    }

    public void clear(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            this.mapYellow.clear();
        } else {
            this.mapBlue.clear();
        }
    }

    public void clear() {
        this.mapYellow.clear();
        this.mapBlue.clear();
    }

    public Set<Pair<TeamColor, Integer>> keySet() {
        Map<Pair<TeamColor, Integer>, T> map = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, T> entry : this.mapBlue.entrySet()) {
            map.put(new Pair<>(TeamColor.BLUE, entry.getKey()), entry.getValue());
        }
        for (Map.Entry<Integer, T> entry : this.mapYellow.entrySet()) {
            map.put(new Pair<>(TeamColor.YELLOW, entry.getKey()), entry.getValue());
        }
        return map.keySet();
    }

    public Set<Integer> keySet(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            return this.mapYellow.keySet();
        }
        return this.mapBlue.keySet();
    }

    public Collection<T> values() {
        Map<Pair<TeamColor, Integer>, T> map = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, T> entry : this.mapBlue.entrySet()) {
            map.put(new Pair<>(TeamColor.BLUE, entry.getKey()), entry.getValue());
        }
        for (Map.Entry<Integer, T> entry : this.mapYellow.entrySet()) {
            map.put(new Pair<>(TeamColor.YELLOW, entry.getKey()), entry.getValue());
        }
        return map.values();
    }

    public Collection<T> values(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            return this.mapYellow.values();
        }
        return this.mapBlue.values();
    }

    public Set<Map.Entry<Pair<TeamColor, Integer>, T>> entrySet() {
        Map<Pair<TeamColor, Integer>, T> map = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, T> entry : this.mapBlue.entrySet()) {
            map.put(new Pair<>(TeamColor.BLUE, entry.getKey()), entry.getValue());
        }
        for (Map.Entry<Integer, T> entry : this.mapYellow.entrySet()) {
            map.put(new Pair<>(TeamColor.YELLOW, entry.getKey()), entry.getValue());
        }
        return map.entrySet();
    }

    public Set<Map.Entry<Integer, T>> entrySet(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            return this.mapYellow.entrySet();
        }
        return this.mapBlue.entrySet();
    }

    public Map<Integer, T> getMap(@Nonnull TeamColor color) {
        if (color.isYellow())
            return this.mapYellow;
        return this.mapBlue;
    }
}
