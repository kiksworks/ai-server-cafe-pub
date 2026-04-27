package ai_server_cafe.gui;

import ai_server_cafe.Main;
import ai_server_cafe.config.Config;
import ai_server_cafe.gui.interfaces.IContainerCafe;
import ai_server_cafe.gui.interfaces.IGraphicalComponent;
import ai_server_cafe.gui.item.Replay0Cafe;
import ai_server_cafe.gui.item.basic.NoneCafe;
import ai_server_cafe.gui.registry.RegistryGraphicalComponents;
import ai_server_cafe.gui.registry.RegistryInputComponents;
import ai_server_cafe.records.AbstractLayerData;
import ai_server_cafe.records.ver0.RecordData0;
import ai_server_cafe.replay.ReplayState;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterCafeStatus;
import ai_server_cafe.updater.UpdaterReplay;
import ai_server_cafe.updater.UpdaterThreadStatus;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.interfaces.IFunction;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class GuiThread extends AbstractLoopThreadCafe {
    private static GuiThread instance = null;
    @SuppressWarnings("unused")
    private static boolean updating = false;
    private double lastDate;
    @SuppressWarnings("unused")
    private double serverStartingTime;
    private boolean isVisible;
    private final Map<Class<? extends IContainerCafe>, LinkedHashMap<String, IGraphicalComponent>> graphicalComponentsMap;
    private GameWindow window;
    private ReplayState lastReplayState = ReplayState.STOP;
    private ReplayState currentReplayState = ReplayState.STOP;

    private GuiThread() {
        super("gui_thread");
        this.isVisible = false;
        this.lastDate = TimeHelper.now();
        this.graphicalComponentsMap = new HashMap<>();
    }

    public static GuiThread getInstance() {
        if (instance == null) {
            instance = new GuiThread();
        }
        return instance;
    }

    synchronized public GuiThread setVisible(boolean value) {
        this.isVisible = value;
        return this;
    }

    @Override
    protected void loop() {
        double nowDate = TimeHelper.now();
        if (nowDate - lastDate >= ConfigManager.getInstance().getFrameTime()) {
            Config config = ConfigManager.getInstance().getConfig();
            UpdaterCafeStatus.getInstance().setFPS(FastMath.max(1.0 / (nowDate - this.lastDate), config.getFrameTime()));
            this.lastDate = nowDate;
            UpdaterThreadStatus.getInstance().addTPSData(this.name, nowDate);
            synchronized (this) {
                this.window.onChangeVisibleType(ConfigManager.getInstance().getGuiVisibleType());
                this.window.updateContents();
                this.currentReplayState = UpdaterReplay.getInstance().getReplayState();
                // GUI Items
                for (Class<? extends IContainerCafe> entry : this.graphicalComponentsMap.keySet()) {
                    for (Map.Entry<String, IGraphicalComponent> entry1 : this.graphicalComponentsMap.get(entry).entrySet()) {
                        entry1.setValue(new NoneCafe());
                    }
                }
                if (ConfigManager.getInstance().isGuiNeedManualUpdate()) {
                    this.window.updateManually();
                    ConfigManager.getInstance().setGuiNeedManualUpdate(false);
                }
                Optional<RecordData0> optRecord = UpdaterReplay.getInstance().getReplayFrameData(RecordData0.class).get();
                if (optRecord.isPresent() && UpdaterReplay.getInstance().getReplayState() != ReplayState.STOP) {
                    if (this.lastReplayState == ReplayState.STOP && this.currentReplayState != ReplayState.STOP) {
                        RegistryGraphicalComponents.init(VisionArea.class, this.graphicalComponentsMap.get(VisionArea.class));
                    }
                    // RecordData0の再生処理(描画処理)
                    final Map<String, Integer> layerData = new HashMap<>();
                    try {
                        for (RecordData0.LayerData0 layer : optRecord.get().layerData0) {
                            // フィルターをかける(描画するレイヤーだけ描画する)
                            if (!replayFilter(layer, config)) {
                                continue;
                            }
                            // 名前が被っているものは番号を付ける
                            // (Ex:blue_obstacle_0, blue_obstacle_1, ...)
                            if (!layerData.containsKey(layer.name)) {
                                layerData.put(layer.name, 0);
                            } else {
                                layerData.put(layer.name, layerData.get(layer.name) + 1);
                            }

                            // paint
                            this.graphicalComponentsMap.get(VisionArea.class)
                                    .putLast(layer.name + "_" + layerData.get(layer.name), new Replay0Cafe(layer));
                        }
                    } catch (NoSuchElementException e) {
                        e.printStackTrace();
                        this.currentReplayState = ReplayState.STOP;
                        UpdaterReplay.getInstance().setReplayState(ReplayState.STOP);
                    }
                } else {
                    // 通常の描画処理
                    if(this.lastReplayState != ReplayState.STOP && this.currentReplayState == ReplayState.STOP) {
                        RegistryGraphicalComponents.init(VisionArea.class, this.graphicalComponentsMap.get(VisionArea.class));
                    }
                    // 描画処理
                    for (Class<? extends IContainerCafe> entry : this.graphicalComponentsMap.keySet()) {
                        RegistryGraphicalComponents.update(entry, this.graphicalComponentsMap.get(entry), ConfigManager.getInstance().getConfig());
                    }
                }
                for (Map.Entry<Class<? extends IContainerCafe>, LinkedHashMap<String, IGraphicalComponent>> entry : this.graphicalComponentsMap.entrySet()) {
                    List<IGraphicalComponent> list = new ArrayList<>(entry.getValue().values());
                    this.window.setGraphicalComponents(list, entry.getKey());
                }
            }
            this.lastReplayState = this.currentReplayState;
            this.window.repaint();
        }
    }

    @Override
    protected void init() {
        this.serverStartingTime = TimeHelper.now();
        IFunction<Void> onExit = new IFunction<Void>() {
            @Override
            @Nonnull
            public Void function(@Nullable Object... args) {
                Main.exit(0, false);
                return null;
            }
        };
        this.window = new GameWindow("AI Server Cafe", 1280, 720, onExit);
        try {
            String path = "/kawasemi_blue2_icon.png";
            InputStream imgStream = getClass().getResourceAsStream(path);
            assert imgStream != null;
            BufferedImage myImg = ImageIO.read(imgStream);
            if (myImg == null) {
                this.logger.info("myImg is null");
            }
            assert myImg != null;
            ImageIcon imageIcon = new ImageIcon(myImg.getScaledInstance(64, 64, Image.SCALE_DEFAULT));
            this.window.setIconImage(imageIcon.getImage());
            if (Taskbar.isTaskbarSupported()) {
                Taskbar tb = Taskbar.getTaskbar();
                if (tb.isSupported(Taskbar.Feature.ICON_IMAGE)) tb.setIconImage(imageIcon.getImage());
                if (tb.isSupported(Taskbar.Feature.ICON_BADGE_TEXT))
                    Taskbar.getTaskbar().setIconBadge("AI Server Cafe");
            } else {
                this.logger.info("taskbar is not supported");
            }
        } catch (NullPointerException | IOException ex) {
            throw new RuntimeException(ex);
        }
        RegistryInputComponents.registerContents(this.window);
        this.window.setVisible(this.isVisible);

        for (Class<? extends IContainerCafe> entry : RegistryGraphicalComponents.getContainers()) {
            this.graphicalComponentsMap.put(entry, new LinkedHashMap<>());
            RegistryGraphicalComponents.init(entry, this.graphicalComponentsMap.get(entry));
        }
    }

    public static void reset() {
        instance.window.removeAll();
        instance.window.setVisible(false);
        instance = null;
    }

    private boolean replayFilter(AbstractLayerData layer, Config config) {
        String[] backgrounds = new String[] {"background", "goalToGoalLine", "centerLine", "fieldLine", "outsideLine",
                "centerCircle", "ourPenalty", "oppositePenalty", "ourGoal", "oppositeGoal", "labelMinX", "labelMaxX",
                "yellow_penalty_circle", "blue_penalty_circle"};
        for(String filter : backgrounds) {
            if (layer.name.contains(filter)) {
                return true;
            }
        }
        for(String filter : config.replayVisible) {
            if (layer.name.contains(filter)) {
                return true;
            }
        }
        return false;
    }
}
