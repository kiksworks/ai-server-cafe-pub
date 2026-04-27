package ai_server_cafe.util.interfaces;

public class WrapperWeakCloneable<T> extends AbstractCloneable {
    private final T t;
    /**
     * 不変クラスのラッパー
     * @param t 不変クラスのインスタンス
     */
    public WrapperWeakCloneable(T t) {
        this.t = t;
    }

    public T get() {
        return this.t;
    }

    @Override
    public WrapperWeakCloneable<T> clone() {
        return new WrapperWeakCloneable<>(this.t);
    }
}
