package ai_server_cafe.filter;

import ai_server_cafe.model.field.AbstractFilteredObject;

/**
 * フィルタの更新タイミングがvisionの更新タイミングと同時
 * @param <T> Vをフィルタリングした結果
 * @param <V> フィルタをかける前
 */
public abstract class AbstractFilterSame<T extends AbstractFilteredObject<V>, V> extends AbstractFilter<T, V> {

}
