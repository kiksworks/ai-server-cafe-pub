package ai_server_cafe.util.interfaces;

import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public class InterfaceHelper {
    public enum Compare {
        EQUALS(0),
        FIRST_LARGER(1),
        SECOND_LARGER(-1);

        final int i;
        Compare(int i) {
            this.i = i;
        }

        public int getValue() {
            return i;
        }
    }
    @Nonnull
    public static <T> Comparator<T> getComparator(IFuncParam1<Double, T> func) {
        return Comparator.comparingDouble(new ToDoubleFunction<T>() {
            @Override
            public double applyAsDouble(T value) {
                return func.function(value);
            }
        });
    }

    /**
     * UpdaterBall用 カメラの境目付近での挙動修正用
     * @param func
     * @return
     * @param <T>
     */
    @Nonnull
    public static <T> Comparator<T> getComparator(IFuncParam2<Boolean, T, T> func) {
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return o1.equals(o2) ? 0 : (func.function(o1, o2) ? -1 : 1);
            }
        };
    }

    @Nonnull
    public static <T> Comparator<T> getStrictComparator(IFuncParam2<Compare, T, T> func) {
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return func.function(o1, o2).getValue();
            }
        };
    }

    @Nonnull
    public static <T> Predicate<T> getPredicate(IFuncParam1<Boolean, T> func) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return func.function(t);
            }
        };
    }

    /*
    @Nonnull
    public static <K, V> Optional<K> getKey(@Nonnull Map<K, V> map, V v) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(v))
                return Optional.of(entry.getKey());
        }
        return Optional.empty();
    }

    @Nonnull
    public static <K, V> Optional<Pair<K, V>> getPair(@Nonnull Map<K, V> map, V v) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(v))
                return Optional.of(new Pair<>(entry.getKey(), entry.getValue()));
        }
        return Optional.empty();
    }
    */
    public static <K, V> List<Pair<K, V>> makePairList(Map<K, V> map) {
        List<Pair<K, V>> result = new ArrayList<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            result.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    @Nonnull
    public static Class<?> convertPrimitive(@Nonnull Class<?> value) {
        if (value == Integer.class)
            return int.class;
        if (value == Float.class)
            return float.class;
        if (value == Double.class)
            return double.class;
        if (value == Byte.class)
            return byte.class;
        if (value == Short.class)
            return short.class;
        if (value == Character.class)
            return char.class;
        if (value == Long.class)
            return long.class;
        if (value == Boolean.class)
            return int.class;
        if (value == Void.class)
            return void.class;
        return value;
    }

    @Nonnull
    public static <T> List<T> findAll(@Nonnull List<T> list, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T value : list) {
            if (predicate.test(value)) {
                result.add(value);
            }
        }
        return result;
    }
}
