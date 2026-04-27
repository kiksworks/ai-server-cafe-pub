package ai_server_cafe.filter;

import ai_server_cafe.model.field.AbstractFilteredObject;

public abstract class AbstractFilterObserver<T extends AbstractFilteredObject<V>, V> extends AbstractFilterSame<T, V> implements IObserver<T> {
}
