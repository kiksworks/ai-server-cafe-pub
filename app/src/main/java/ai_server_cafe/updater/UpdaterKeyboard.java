package ai_server_cafe.updater;

import ai_server_cafe.config.Config;
import ai_server_cafe.device.keyboard.StatusKeyboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class UpdaterKeyboard {
    private final Logger logger;
    private static UpdaterKeyboard instance = null;
    // KeyCodeと押されたかのbooleanのマップ
    private final Map<Integer, Boolean> keyStatus;
    // ショートカットキー登録名とKeyCodeのマップ
    private final Map<String, Integer> keyMap;

    private UpdaterKeyboard() {
        this.logger = LogManager.getLogger("updater-keyboard");
        this.keyMap = new HashMap<>();
        this.keyStatus = new HashMap<>();
    }

    /**
     * ショートカットキー登録名とKeyCodeを紐づける
     */
    public synchronized void init() {
        final Config.KeyboardConfig ck = ConfigManager.getInstance().getConfig().keyboardConfig;
        for (Field f : ck.getClass().getFields()) {
            if (f == null) continue;
            if (f.getType() == String.class) {
                try {
                    String value = (String) f.get(ck);
                    this.keyMap.put(f.getName(), this.registerNumber(value));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     *
     * @param str 対応させる文字列
     * @return strに対応したKeyEventの整数値
     */
    private int registerNumber(@Nonnull String str) {
        switch (str) {
            case "q" : return KeyEvent.VK_Q;
            case "w" : return KeyEvent.VK_W;
            case "e" : return KeyEvent.VK_E;
            case "r" : return KeyEvent.VK_R;
            case "t" : return KeyEvent.VK_T;
            case "y" : return KeyEvent.VK_Y;
            case "u" : return KeyEvent.VK_U;
            case "i" : return KeyEvent.VK_I;
            case "o" : return KeyEvent.VK_O;
            case "p" : return KeyEvent.VK_P;
            case "a" : return KeyEvent.VK_A;
            case "s" : return KeyEvent.VK_S;
            case "d" : return KeyEvent.VK_D;
            case "f" : return KeyEvent.VK_F;
            case "g" : return KeyEvent.VK_G;
            case "h" : return KeyEvent.VK_H;
            case "j" : return KeyEvent.VK_J;
            case "k" : return KeyEvent.VK_K;
            case "l" : return KeyEvent.VK_L;
            case "z" : return KeyEvent.VK_Z;
            case "x" : return KeyEvent.VK_X;
            case "c" : return KeyEvent.VK_C;
            case "v" : return KeyEvent.VK_V;
            case "b" : return KeyEvent.VK_B;
            case "n" : return KeyEvent.VK_N;
            case "m" : return KeyEvent.VK_M;
            case "shift" : return KeyEvent.VK_SHIFT;
            case "ctrl" : return KeyEvent.VK_CONTROL;
            case "up" : return KeyEvent.VK_UP;
            case "down" : return KeyEvent.VK_DOWN;
            case "left" : return KeyEvent.VK_LEFT;
            case "right" : return KeyEvent.VK_RIGHT;
            case "space" : return KeyEvent.VK_SPACE;
            case "esc" : return KeyEvent.VK_ESCAPE;
            default : {
                this.logger.warn("{} is not registered", str);
                return 0;
            }
        }
    }

    public synchronized static UpdaterKeyboard getInstance() {
        if(instance == null) {
            instance = new UpdaterKeyboard();
        }
        return instance;
    }

    synchronized public void setKeyStatus(int keyCode, boolean status) {
        this.keyStatus.put(keyCode, status);
    }

    synchronized public void resetKey() {
        for(Integer key : this.keyStatus.keySet()) {
            this.keyStatus.put(key, false);
        }
    }

    @Nonnull
    synchronized public StatusKeyboard update() {
        return new StatusKeyboard(this.keyMap, this.keyStatus);
    }

    synchronized public boolean isKeyPressed(String key) {
        return this.keyStatus.getOrDefault(this.registerNumber(key), false);
    }
}
