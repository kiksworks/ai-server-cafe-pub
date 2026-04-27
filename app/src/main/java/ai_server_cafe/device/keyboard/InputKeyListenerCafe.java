package ai_server_cafe.device.keyboard;

import ai_server_cafe.updater.UpdaterKeyboard;

import javax.annotation.Nonnull;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Cafeでキーボード入力を実装するクラス
 */
public class InputKeyListenerCafe implements KeyListener {
    @Override
    public void keyPressed(@Nonnull KeyEvent e) {
        int code = e.getKeyCode();
        UpdaterKeyboard.getInstance().setKeyStatus(code, true);
    }

    @Override
    public void keyReleased(@Nonnull KeyEvent e) {
        int code = e.getKeyCode();
        UpdaterKeyboard.getInstance().setKeyStatus(code, false);
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
