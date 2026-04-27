package ai_server_cafe.util.interfaces;

import javax.annotation.Nonnull;
import java.util.List;

public class CompareHelper {
    /**
     * list, list2の比較をします。2つのlistの要素数とそれらの要素の参照が互いに同じかどうかを対象とします
     * @param list
     * @param list2
     * @return
     * @param <T>
     */
    public static <T> boolean compareList(@Nonnull List<T> list, @Nonnull List<T> list2) {
        if (list.size() != list2.size()) {
            return false;
        }
        for (T t : list) {
            if (!list2.contains(t)) {
                return false;
            }
        }
        return true;
    }
}
