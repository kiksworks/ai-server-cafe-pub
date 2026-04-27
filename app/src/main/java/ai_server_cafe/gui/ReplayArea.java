package ai_server_cafe.gui;

import ai_server_cafe.device.keyboard.InputKeyListenerCafe;
import ai_server_cafe.device.keyboard.StatusKeyboard;
import ai_server_cafe.gui.interfaces.AbstractPanelCafe;
import ai_server_cafe.replay.ReplayState;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterKeyboard;
import ai_server_cafe.updater.UpdaterReplay;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * ReplayCafe関係のボタン(Pause, Startなど)の処理を書くクラス
 */
public class ReplayArea extends AbstractPanelCafe {

    public ReplayArea(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void initPaint(Graphics2D graphics2D) {
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    @Override
    public boolean isVisibleConfig() {
        return true;
    }

    @Override
    public void onResizePanel(int width, int height) {}

    @Override
    public void onResize(int newX, int newY) {}

    @Override
    public void mousePressed(MouseEvent e) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
        this.requestFocus();
        this.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // open folder後に再度open folderが開かれるのを防ぐ用
                UpdaterKeyboard.getInstance().resetKey();
            }

            @Override
            public void focusLost(FocusEvent e) {
                // open folder後に再度open folderが開かれるのを防ぐ用
                UpdaterKeyboard.getInstance().resetKey();
            }
        });
        this.addKeyListener(new InputKeyListenerCafe() {
            @Override
            public void keyPressed(@NotNull KeyEvent e) {
                super.keyPressed(e);
                StatusKeyboard status = UpdaterKeyboard.getInstance().update();
                if(UpdaterKeyboard.getInstance().isKeyPressed("ctrl") && status.getStatus("fastForward")) {
                    final long commonFrame = UpdaterReplay.getInstance().getReplayFrame();
                    UpdaterReplay.getInstance().setReplayFrame(commonFrame + 1);
                } else if(status.getStatus("fastForward")) {
                    final long commonFrame = UpdaterReplay.getInstance().getReplayFrame();
                    final long fastFrame = ConfigManager.getInstance().getConfig().fastFrame;
                    UpdaterReplay.getInstance().setReplayFrame(commonFrame + fastFrame);
                }
                if(UpdaterKeyboard.getInstance().isKeyPressed("ctrl") && status.getStatus("rewind")) {
                    final long commonFrame = UpdaterReplay.getInstance().getReplayFrame();
                    UpdaterReplay.getInstance().setReplayFrame(commonFrame - 1);
                } else if(status.getStatus("rewind")) {
                    final long commonFrame = UpdaterReplay.getInstance().getReplayFrame();
                    final long fastFrame = ConfigManager.getInstance().getConfig().fastFrame;
                    UpdaterReplay.getInstance().setReplayFrame(commonFrame - fastFrame);
                }
                if(status.getStatus("replayPause")) {
                    switch(UpdaterReplay.getInstance().getReplayState()) {
                        case PAUSE : {
                            if(UpdaterReplay.getInstance().getReplayFrame() ==
                                    UpdaterReplay.getInstance().getReplayLastFrame()) {
                                UpdaterReplay.getInstance().setReplayFrame(UpdaterReplay.getInstance().getReplayInitialFrame());
                            }
                            UpdaterReplay.getInstance().setReplayState(ReplayState.REPLAY);
                            break;
                        }
                        case REPLAY : {
                            UpdaterReplay.getInstance().setReplayState(ReplayState.PAUSE);
                            break;
                        }
                    }
                }
            }
        });
    }
}
