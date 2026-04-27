package ai_server_cafe.util.interfaces;

import java.util.HashMap;
import java.util.Map;

public class WrapperWeakCloneableMap<K, V> extends AbstractCloneable {
    private Map<K, V> map;
    public WrapperWeakCloneableMap(Map<K, V> map) {
        this.map = new HashMap<>(map);
    }

    @Override
    public WrapperWeakCloneableMap<K, V> clone() {
        return new WrapperWeakCloneableMap<>(this.map);
    }

    public Map<K, V> get() {
        return this.map;
    }
}
