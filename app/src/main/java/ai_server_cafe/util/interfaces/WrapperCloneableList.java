package ai_server_cafe.util.interfaces;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class WrapperCloneableList<T extends AbstractCloneable> extends AbstractCloneable {
    private final List<T> list;

    @SuppressWarnings("unchecked")
    public WrapperCloneableList(@Nonnull List<T> list) {
        this.list = new ArrayList<>();
        for (T t : list) {
            this.list.add((T)t.clone());
        }
    }

    @Override
    @Nonnull
    @SuppressWarnings("unchecked")
    public WrapperCloneableList<T> clone() {
        List<T> newList = new ArrayList<>();
        for (T t : list) {
            newList.add((T)t.clone());
        }
        return new WrapperCloneableList<>(newList);
    }

    @Nonnull
    public List<T> get() {
        return this.list;
    }
}
