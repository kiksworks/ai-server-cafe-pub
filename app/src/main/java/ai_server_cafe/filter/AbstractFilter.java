package ai_server_cafe.filter;

import ai_server_cafe.model.field.AbstractFilteredObject;

import java.util.Optional;

/**
 * フィルタの基底クラス
 * @param <T> Vをフィルタリングした結果
 * @param <V> フィルタをかける前
 */
public abstract class AbstractFilter<T extends AbstractFilteredObject<V>, V> {
    protected double delay;
    public abstract Optional<T> updateRaw(Optional<V> rawValue, double updateTime);
    public void setDelay(double time) {
        this.delay = time;
    }
}

