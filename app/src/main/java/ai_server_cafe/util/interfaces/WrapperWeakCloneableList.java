package ai_server_cafe.util.interfaces;

import java.util.ArrayList;
import java.util.List;

public class WrapperWeakCloneableList<T> extends AbstractCloneable {
    private List<T> list;
    public WrapperWeakCloneableList(List<T> list) {
        this.list = new ArrayList<>(list);
    }

    @Override
    public WrapperWeakCloneableList<T> clone() {
        return new WrapperWeakCloneableList<>(this.list);
    }

    public List<T> get() {
        return this.list;
    }
}
