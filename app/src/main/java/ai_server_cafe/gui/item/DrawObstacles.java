package ai_server_cafe.gui.item;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.VisionArea;
import ai_server_cafe.gui.interfaces.IContainerCafe;
import ai_server_cafe.gui.interfaces.IGraphicalComponent;
import ai_server_cafe.gui.item.basic.NoneCafe;
import ai_server_cafe.gui.item.basic.ObstacleCafe;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterCurrentObstacle;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.gui.ColorHelper;
import ai_server_cafe.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

public class DrawObstacles implements IItemDraw {
    @Override
    public void init(Class<? extends IContainerCafe> area,
            LinkedHashMap<String, IGraphicalComponent> graphicalComponents) {
        // Obstacles
        for (int blueRobot = 0; blueRobot < ConfigManager.MAX_ROBOTS; blueRobot++) {
            graphicalComponents.putLast("blue_obstacle_" + blueRobot, new NoneCafe());
        }
        for (int yellowRobot = 0; yellowRobot < ConfigManager.MAX_ROBOTS; yellowRobot++) {
            graphicalComponents.putLast("yellow_obstacle_" + yellowRobot, new NoneCafe());
        }
    }

    @Override
    public void update(Class<? extends IContainerCafe> area,
            @Nonnull LinkedHashMap<String, IGraphicalComponent> graphicalComponents, @Nonnull Config config) {
        boolean worldInvert = config.visibility.invertVisible;
        World world = UpdaterWorld.getInstance().getWorld(worldInvert);
        if (area != VisionArea.class) {
            return;
        }
        // Obstacles
        for (Map.Entry<Integer, IntegratedRobot> blueIRobot : world.getFriendlyRobotMap(TeamColor.BLUE).entrySet()) {
            if (MathHelper.toList(config.visibility.blueObstacle).contains(blueIRobot.getKey())) {
                graphicalComponents.put("blue_obstacle_" + blueIRobot.getKey(),
                        new ObstacleCafe(ColorHelper.OBSTACLE_SCARLET,
                                UpdaterCurrentObstacle.getInstance().get(TeamColor.BLUE, blueIRobot.getKey()).get(),
                                isInvertView(TeamColor.BLUE, config)));
            }
        }
        for (Map.Entry<Integer, IntegratedRobot> yellowIRobot : world.getFriendlyRobotMap(TeamColor.YELLOW)
                .entrySet()) {
            if (MathHelper.toList(config.visibility.yellowObstacle).contains(yellowIRobot.getKey())) {
                graphicalComponents.put("yellow_obstacle_" + yellowIRobot.getKey(),
                        new ObstacleCafe(ColorHelper.OBSTACLE_SCARLET,
                                UpdaterCurrentObstacle.getInstance().get(TeamColor.YELLOW, yellowIRobot.getKey()).get(),
                                isInvertView(TeamColor.YELLOW, config)));
            }
        }
    }

    @Override
    public RecordData1 putRecord(@NotNull RecordData1 layer) {
        for (int yellowRobot = 0; yellowRobot < ConfigManager.MAX_ROBOTS; yellowRobot++) {
            layer.setObstacleData(TeamColor.YELLOW, yellowRobot, UpdaterCurrentObstacle.getInstance().get(TeamColor.YELLOW, yellowRobot).get());
        }
        for (int blueRobot = 0; blueRobot < ConfigManager.MAX_ROBOTS; blueRobot++) {
            layer.setObstacleData(TeamColor.BLUE, blueRobot, UpdaterCurrentObstacle.getInstance().get(TeamColor.BLUE, blueRobot).get());
        }
        return layer;
    }
}
