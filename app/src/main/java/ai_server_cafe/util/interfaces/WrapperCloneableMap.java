package ai_server_cafe.util.interfaces;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class WrapperCloneableMap<K, V extends AbstractCloneable> extends AbstractCloneable {
    private final Map<K, V> map;

    @SuppressWarnings("unchecked")
    public WrapperCloneableMap(@Nonnull Map<K, V> map) {
        this.map = new HashMap<>();
        for (Map.Entry<K, V> t : map.entrySet()) {
            this.map.put(t.getKey(), (V)t.getValue().clone());
        }
    }

    @Override
    @Nonnull
    @SuppressWarnings("unchecked")
    public WrapperCloneableMap<K, V> clone() {
        Map<K, V> hashMap = new HashMap<>();
        for (Map.Entry<K, V> t : this.map.entrySet()) {
            hashMap.put(t.getKey(), (V)t.getValue().clone());
        }
        return new WrapperCloneableMap<>(hashMap);
    }

    @Nonnull
    public Map<K, V> get() {
        return this.map;
    }
}
