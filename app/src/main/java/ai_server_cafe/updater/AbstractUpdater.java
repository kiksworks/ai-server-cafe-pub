package ai_server_cafe.updater;

import ai_server_cafe.model.field.FieldObject;

public abstract class AbstractUpdater<T extends FieldObject> {
    /**
     * 最終的な値
     */
    protected T value;

    public AbstractUpdater(T init) {
        this.value = init;
    }

    /**
     * 最終的な値
     * デフォルトは clone() メソッドでdeep copyした値を返す
     */
    @SuppressWarnings("unchecked")
    synchronized public T getValue() {
        return (T)this.value.clone();
    }
}
