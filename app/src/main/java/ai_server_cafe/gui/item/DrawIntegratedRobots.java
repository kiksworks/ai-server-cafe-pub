package ai_server_cafe.gui.item;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.VisionArea;
import ai_server_cafe.gui.interfaces.IContainerCafe;
import ai_server_cafe.gui.interfaces.IGraphicalComponent;
import ai_server_cafe.gui.item.basic.ActionCafe;
import ai_server_cafe.gui.item.basic.LineCafe;
import ai_server_cafe.gui.item.basic.NoneCafe;
import ai_server_cafe.gui.item.basic.RobotCafe;
import ai_server_cafe.gui.item.basic.RoleBoxesCafe;
import ai_server_cafe.gui.item.basic.RoleCafe;
import ai_server_cafe.gui.item.basic.RoleTargetsCafe;
import ai_server_cafe.gui.item.basic.StringCafe;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.records.ver1.WorldWrapper;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.gui.ColorHelper;
import ai_server_cafe.util.interfaces.SerializableJson;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class DrawIntegratedRobots implements IItemDraw {
    @Override
    public void init(Class<? extends IContainerCafe> area,
            LinkedHashMap<String, IGraphicalComponent> graphicalComponents) {
        // Role
        for (int blueRobot = 0; blueRobot < ConfigManager.MAX_ROBOTS; blueRobot++) {
            graphicalComponents.putLast("blue_role_" + blueRobot, new NoneCafe());
        }
        for (int yellowRobot = 0; yellowRobot < ConfigManager.MAX_ROBOTS; yellowRobot++) {
            graphicalComponents.putLast("yellow_role_" + yellowRobot, new NoneCafe());
        }
        // RoleTargets
        for (int blueRobot = 0; blueRobot < ConfigManager.MAX_ROBOTS; blueRobot++) {
            graphicalComponents.putLast("blue_role_targets_" + blueRobot, new NoneCafe());
        }
        for (int yellowRobot = 0; yellowRobot < ConfigManager.MAX_ROBOTS; yellowRobot++) {
            graphicalComponents.putLast("yellow_role_targets_" + yellowRobot, new NoneCafe());
        }
        // RoleBoxes
        for (int blueRobot = 0; blueRobot < ConfigManager.MAX_ROBOTS; blueRobot++) {
            graphicalComponents.putLast("blue_role_boxes_" + blueRobot, new NoneCafe());
        }
        for (int yellowRobot = 0; yellowRobot < ConfigManager.MAX_ROBOTS; yellowRobot++) {
            graphicalComponents.putLast("yellow_role_boxes_" + yellowRobot, new NoneCafe());
        }
        // Action
        for (int blueRobot = 0; blueRobot < ConfigManager.MAX_ROBOTS; blueRobot++) {
            graphicalComponents.putLast("blue_action_" + blueRobot, new NoneCafe());
        }
        for (int yellowRobot = 0; yellowRobot < ConfigManager.MAX_ROBOTS; yellowRobot++) {
            graphicalComponents.putLast("yellow_action_" + yellowRobot, new NoneCafe());
        }
        // ロボット
        for (int blueRobot = 0; blueRobot < ConfigManager.MAX_ROBOTS; blueRobot++) {
            graphicalComponents.putLast("blue_robot_" + blueRobot, new NoneCafe());
        }
        for (int yellowRobot = 0; yellowRobot < ConfigManager.MAX_ROBOTS; yellowRobot++) {
            graphicalComponents.putLast("yellow_robot_" + yellowRobot, new NoneCafe());
        }
        // ID
        for (int blueRobot = 0; blueRobot < ConfigManager.MAX_ROBOTS; blueRobot++) {
            graphicalComponents.putLast("blue_robot_id" + blueRobot, new NoneCafe());
        }
        for (int yellowRobot = 0; yellowRobot < ConfigManager.MAX_ROBOTS; yellowRobot++) {
            graphicalComponents.putLast("yellow_robot_id" + yellowRobot, new NoneCafe());
        }
        // Robot speed string
        for (int blueRobot = 0; blueRobot < ConfigManager.MAX_ROBOTS; blueRobot++) {
            graphicalComponents.putLast("blue_speed" + blueRobot, new NoneCafe());
        }
        for (int yellowRobot = 0; yellowRobot < ConfigManager.MAX_ROBOTS; yellowRobot++) {
            graphicalComponents.putLast("yellow_speed" + yellowRobot, new NoneCafe());
        }
        // Robot speed line
        for (int blueRobot = 0; blueRobot < ConfigManager.MAX_ROBOTS; blueRobot++) {
            graphicalComponents.putLast("blue_robot_line_" + blueRobot, new NoneCafe());
        }
        for (int yellowRobot = 0; yellowRobot < ConfigManager.MAX_ROBOTS; yellowRobot++) {
            graphicalComponents.putLast("yellow_robot_line_" + yellowRobot, new NoneCafe());
        }
    }

    @Override
    public void update(Class<? extends IContainerCafe> area,
            LinkedHashMap<String, IGraphicalComponent> graphicalComponents, @Nonnull Config config) {
        boolean worldInvert = config.visibility.invertVisible;
        World world = UpdaterWorld.getInstance().getWorld(worldInvert);
        if (area != VisionArea.class) {
            return;
        }
        // ロボット
        for (Map.Entry<Integer, IntegratedRobot> blueIRobot : world.getFriendlyRobotMap(TeamColor.BLUE).entrySet()) {
            Pair<Integer, FilteredRobot> blueRobot = new Pair<>(blueIRobot.getKey(), blueIRobot.getValue().getRobot());
            graphicalComponents.put("blue_robot_" + blueRobot.getKey(),
                    new RobotCafe(TeamColor.BLUE, blueIRobot.getValue()));
            graphicalComponents.put("blue_robot_id" + blueRobot.getKey(),
                    new StringCafe(ColorHelper.LINE_WHITE, (int) blueRobot.getValue().getX() - 60,
                            (int) blueRobot.getValue().getY() + 100, String.valueOf(blueRobot.getKey().intValue()),
                            200));
            if (config.visibility.robotSpeedVisible) {
                // ロボット速度の計算
                int robotSpeed = (int) blueRobot.getSecond().velocity().getNorm();
                // ロボット速度の色とラベルサイズを調整
                Color robotSpeedColor = Color.LIGHT_GRAY;
                int labelSize = 170;
                if (robotSpeed >= 3000) { // 閾値
                    labelSize += 10;
                    robotSpeedColor = Color.RED;
                }

                // ロボット速度の数値ラベル
                graphicalComponents.put("blue_speed" + blueRobot.getKey(),
                        new StringCafe(ColorHelper.LINE_WHITE, (int) blueRobot.getValue().getX() - 60,  // ラベルのX位置
                                (int) blueRobot.getValue().getY() - 100, // ラベルのY位置
                                String.valueOf(robotSpeed),             // 速度の値
                                labelSize                               // ラベルのサイズ
                        ));

                // 速度ベクトルのスケーリング
                final double scale = 1.0; // スケール調整用（視認性を向上させる）
                double scaledVx = blueRobot.getValue().getVx() * scale;
                double scaledVy = blueRobot.getValue().getVy() * scale;

                // ロボット速度ベクトルの描画
                graphicalComponents.put("blue_velocity_line_" + blueRobot.getKey(),
                        new LineCafe(blueRobot.getValue().getX(),                           // 始点X
                                blueRobot.getValue().getY(),                           // 始点Y
                                blueRobot.getValue().getX() + scaledVx,               // 終点X（スケール適用）
                                blueRobot.getValue().getY() + scaledVy,               // 終点Y（スケール適用）
                                robotSpeedColor,                                       // 線の色
                                10.0F                                                 // 線の太さ
                        ));
            }
            if (config.visibility.roleVisible) {
                graphicalComponents.put("blue_role_" + blueIRobot.getKey(),
                        new RoleCafe(ColorHelper.ROBOT_BLUE, blueIRobot.getValue()));
            }
            if (config.visibility.targetsVisible) {
                graphicalComponents.put("blue_roleTargets_" + blueIRobot.getKey(),
                        new RoleTargetsCafe(ColorHelper.ROBOT_BLUE, blueIRobot.getValue(),
                                isInvertView(TeamColor.BLUE, config)));
            }
            if (config.visibility.waiterBoxVisible) {
                graphicalComponents.put("blue_roleBoxes_" + blueIRobot.getKey(),
                        new RoleBoxesCafe(ColorHelper.TARGET_EMERALD, blueIRobot.getValue(),
                                isInvertView(TeamColor.BLUE, config)));
            }
            if (config.visibility.actionVisible) {
                graphicalComponents.put("blue_action_" + blueIRobot.getKey(),
                        new ActionCafe(ColorHelper.ROBOT_BLUE, blueIRobot.getValue()));
            }
        }
        for (Map.Entry<Integer, IntegratedRobot> yellowIRobot : world.getFriendlyRobotMap(TeamColor.YELLOW)
                .entrySet()) {
            Pair<Integer, FilteredRobot> yellowRobot =
                    new Pair<>(yellowIRobot.getKey(), yellowIRobot.getValue().getRobot());
            graphicalComponents.put("yellow_robot_" + yellowRobot.getKey(),
                    new RobotCafe(TeamColor.YELLOW, yellowIRobot.getValue()));
            graphicalComponents.put("yellow_robot_id" + yellowRobot.getKey(),
                    new StringCafe(ColorHelper.LINE_WHITE, (int) yellowRobot.getValue().getX() - 60,
                            (int) yellowRobot.getValue().getY() + 100, String.valueOf(yellowRobot.getKey().intValue()),
                            200));
            if (config.visibility.robotSpeedVisible) {
                // ロボット速度の計算
                int robotSpeed = (int) yellowRobot.getValue().velocity().getNorm();

                // ロボット速度の色とラベルサイズを調整
                Color robotSpeedColor = Color.LIGHT_GRAY;
                int labelSize = 170;
                if (robotSpeed >= 3000) { // 閾値
                    labelSize += 10;
                    robotSpeedColor = Color.RED;
                }

                // ロボット速度の数値ラベル
                graphicalComponents.put("yellow_speed" + yellowRobot.getKey(),
                        new StringCafe(ColorHelper.LINE_WHITE, (int) yellowRobot.getValue().getX() - 60,  // ラベルのX位置
                                (int) yellowRobot.getValue().getY() - 100, // ラベルのY位置
                                String.valueOf(robotSpeed),             // 速度の値
                                labelSize                               // ラベルのサイズ
                        ));

                // 速度ベクトルのスケーリング
                final double scale = 1.0; // スケール調整用（視認性を向上させる）
                double scaledVx = yellowRobot.getValue().getVx() * scale;
                double scaledVy = yellowRobot.getValue().getVy() * scale;

                // ロボット速度ベクトルの描画
                graphicalComponents.put("yellow_velocity_line_" + yellowRobot.getKey(),
                        new LineCafe(yellowRobot.getValue().getX(),                           // 始点X
                                yellowRobot.getValue().getY(),                           // 始点Y
                                yellowRobot.getValue().getX() + scaledVx,               // 終点X（スケール適用）
                                yellowRobot.getValue().getY() + scaledVy,               // 終点Y（スケール適用）
                                robotSpeedColor,                                       // 線の色
                                10.0F                                                 // 線の太さ
                        ));
            }
            if (config.visibility.roleVisible) {
                graphicalComponents.put("yellow_role_" + yellowIRobot.getKey(),
                        new RoleCafe(ColorHelper.ROBOT_BLUE, yellowIRobot.getValue()));
            }
            if (config.visibility.targetsVisible) {
                graphicalComponents.put("yellow_roleTargets_" + yellowIRobot.getKey(),
                        new RoleTargetsCafe(ColorHelper.ROBOT_YELLOW, yellowIRobot.getValue(),
                                isInvertView(TeamColor.YELLOW, config)));
            }
            if (config.visibility.waiterBoxVisible) {
                graphicalComponents.put("yellow_roleBoxes_" + yellowIRobot.getKey(),
                        new RoleBoxesCafe(ColorHelper.TARGET_EMERALD, yellowIRobot.getValue(),
                                isInvertView(TeamColor.YELLOW, config)));
            }
            if (config.visibility.actionVisible) {
                graphicalComponents.put("yellow_action_" + yellowIRobot.getKey(),
                        new ActionCafe(ColorHelper.ROBOT_YELLOW, yellowIRobot.getValue()));
            }
        }
    }

    @Override
    public RecordData1 putRecord(@NotNull RecordData1 layer) {
        World world = SerializableJson.deserializeJson(layer.worldData);
        if (world == null) {
            world = new World();
        }
        WorldWrapper worldWrapper = new WorldWrapper(world);
        worldWrapper.blueRobotMap = UpdaterWorld.getInstance().getWorld(false).getFriendlyRobotMap(TeamColor.BLUE);
        worldWrapper.yellowRobotMap = UpdaterWorld.getInstance().getWorld(false).getFriendlyRobotMap(TeamColor.YELLOW);
        world = worldWrapper.getFixed();
        layer.worldData = SerializableJson.serializeJson(world);
        return layer;
    }
}
