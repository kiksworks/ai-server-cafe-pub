package ai_server_cafe.updater;

import ai_server_cafe.filter.AbstractFilterManual;
import ai_server_cafe.filter.AbstractFilterSame;
import ai_server_cafe.model.field.AbstractFilteredObject;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.IFunction;
import ai_server_cafe.util.interfaces.InterfaceHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractFilteredUpdater<T extends AbstractFilteredObject<V>, V> extends AbstractUpdater<T> {
    protected Optional<AbstractFilterSame<T, V>> filterSame;
    protected Optional<AbstractFilterManual<T, V>> filterManual;
    protected Optional<IFunction<AbstractFilterSame<T, V>>> initializeFilterFunc;
    protected double updateTime;

    public AbstractFilteredUpdater(T init) {
        super(init);
        this.filterManual = Optional.empty();
        this.filterSame = Optional.empty();
        this.initializeFilterFunc = Optional.empty();
        this.updateTime = 0.0;
    }

    synchronized public void setFilterSame(AbstractFilterSame<T, V> filterSame) {
        this.filterManual = Optional.empty();
        this.filterSame = Optional.of(filterSame);
    }

    synchronized public void setFilterManual(@Nonnull AbstractFilterManual<T, V> filterManual) {
        this.filterSame = Optional.empty();
        filterManual.setWriterFunc(new IFuncParam1<Void, Optional<T>>() {
            @Override
            public Void function(Optional<T> t) {
                AbstractFilteredUpdater.this.updateTime = TimeHelper.now();
                if (t.isPresent()) {
                    AbstractFilteredUpdater.this.value = t.get();
                    AbstractFilteredUpdater.this.value.setLost(false);
                } else {
                    AbstractFilteredUpdater.this.value.setLost(true);
                }
                return null;
            }
        });
        this.filterManual = Optional.of(filterManual);
    }

    synchronized public void clearFilters() {
        this.filterSame = Optional.empty();
        this.filterManual = Optional.empty();
    }

    synchronized public void setDefaultFilter(Class<? extends AbstractFilterSame<T, V>> clazz, Object... arguments) {
        this.initializeFilterFunc = Optional.of(new IFunction<AbstractFilterSame<T, V>>() {
            @Override
            public AbstractFilterSame<T, V> function(@Nullable Object... args) {
                List<Class<?>> classes = new ArrayList<>();
                for (Object o : arguments) {
                    classes.add(InterfaceHelper.convertPrimitive(o.getClass()));
                }
                try {
                    Class<?>[] classes1 = new Class[classes.size()];
                    classes.toArray(classes1);
                    return clazz.getConstructor(classes1).newInstance(arguments);
                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException |
                         InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    synchronized public void clearDefaultFilter() {
        this.initializeFilterFunc = Optional.empty();
    }

    synchronized public void clearAllFilters() {
        this.filterSame = Optional.empty();
        this.filterManual = Optional.empty();
        this.initializeFilterFunc = Optional.empty();
    }
}
