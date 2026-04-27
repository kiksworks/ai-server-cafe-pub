package ai_server_cafe.device.keyboard;

import ai_server_cafe.util.interfaces.AbstractCloneable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * キーボードによる入力データ
 */
public class StatusKeyboard extends AbstractCloneable {
    public static final StatusKeyboard EMPTY = new StatusKeyboard(new HashMap<>(), new HashMap<>());
    private final Map<String, Boolean> keyboardStatus;

    public StatusKeyboard(@Nonnull Map<String, Integer> keyMap, @Nonnull Map<Integer, Boolean> keyStatus) {
        this.keyboardStatus = new HashMap<>();
        for (String key : keyMap.keySet()) {
            this.keyboardStatus.put(key, keyStatus.getOrDefault(keyMap.get(key), false));
        }
    }

    /**
     *
     * @return key名で登録しているキーボードが押されているかチェック
     * (未登録の場合はfalseを返す)
     */
    public boolean getStatus(String key) {
        return this.keyboardStatus.getOrDefault(key, false);
    }

    @Override
    public StatusKeyboard clone() {
        StatusKeyboard result = new StatusKeyboard(new HashMap<>(), new HashMap<>());
        result.keyboardStatus.putAll(this.keyboardStatus);
        return result;
    }
}
