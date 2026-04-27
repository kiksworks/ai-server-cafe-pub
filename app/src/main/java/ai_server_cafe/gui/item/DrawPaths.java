package ai_server_cafe.gui.item;

import ai_server_cafe.config.Config;
import ai_server_cafe.game.planner.path.PathSide;
import ai_server_cafe.gui.VisionArea;
import ai_server_cafe.gui.interfaces.IContainerCafe;
import ai_server_cafe.gui.interfaces.IGraphicalComponent;
import ai_server_cafe.gui.item.basic.NoneCafe;
import ai_server_cafe.gui.item.basic.PathCafe;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.records.ver1.PathPlannerWrapper;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterPathPlanner;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.gui.ColorHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DrawPaths implements IItemDraw {
    private static Optional<List<Vector2D>> getPath(@Nonnull Optional<List<PathSide>> paths) {
        if (paths.isEmpty()) return Optional.empty();
        List<Vector2D> result = new ArrayList<>();
        for (PathSide ps : paths.get()) {
            result.add(ps.side);
        }
        return Optional.of(result);
    }

    @Override
    public void init(Class<? extends IContainerCafe> area,
            LinkedHashMap<String, IGraphicalComponent> graphicalComponents) {
        if (area != VisionArea.class) {
            return;
        }
        // Path
        for (int blueRobot = 0; blueRobot < ConfigManager.MAX_ROBOTS; blueRobot++) {
            graphicalComponents.putLast("blue_path_" + blueRobot, new NoneCafe());
        }
        for (int yellowRobot = 0; yellowRobot < ConfigManager.MAX_ROBOTS; yellowRobot++) {
            graphicalComponents.putLast("yellow_path_" + yellowRobot, new NoneCafe());
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
        for (Map.Entry<Integer, IntegratedRobot> blueIRobot : world.getFriendlyRobotMap(TeamColor.BLUE).entrySet()) {
            Pair<Integer, FilteredRobot> blueRobot = new Pair<>(blueIRobot.getKey(), blueIRobot.getValue().getRobot());
            if (config.visibility.pathVisible) {
                if (blueIRobot.getValue().getAction().isPresent() &&
                        blueIRobot.getValue().getAction().get().getName().contains("with_planner")) {
                    graphicalComponents.put("blue_path_" + blueRobot.getKey(), new PathCafe(
                            UpdaterPathPlanner.getInstance().hasResult(TeamColor.BLUE, blueRobot.getKey()) ?
                                    ColorHelper.VELOCITY_AQUA : ColorHelper.PATH_GOLD, getPath(Optional.of(
                            UpdaterPathPlanner.getInstance().getResult(TeamColor.BLUE, blueRobot.getKey()).get())),
                            blueRobot.getValue().position(), 40.0F, isInvertView(TeamColor.BLUE, config),
                            config.pathPlannerConfig.plannerVisibility.temporaryDestinationView ?
                                    UpdaterPathPlanner.getInstance().getNextVel(TeamColor.BLUE, blueRobot.getKey()).get() : null));
                }
            }
        }
        for (Map.Entry<Integer, IntegratedRobot> yellowIRobot : world.getFriendlyRobotMap(TeamColor.YELLOW)
                .entrySet()) {
            Pair<Integer, FilteredRobot> yellowRobot =
                    new Pair<>(yellowIRobot.getKey(), yellowIRobot.getValue().getRobot());
            if (config.visibility.pathVisible) {
                if (yellowIRobot.getValue().getAction().isPresent() &&
                        yellowIRobot.getValue().getAction().get().getName().contains("with_planner")) {
                    graphicalComponents.put("yellow_path_" + yellowRobot.getKey(), new PathCafe(
                            UpdaterPathPlanner.getInstance().hasResult(TeamColor.YELLOW, yellowRobot.getKey()) ?
                                    ColorHelper.VELOCITY_PEONY : ColorHelper.PATH_GOLD, getPath(Optional.of(
                            UpdaterPathPlanner.getInstance().getResult(TeamColor.YELLOW, yellowRobot.getKey()).get())),
                            yellowRobot.getValue().position(), 40.0F, isInvertView(TeamColor.YELLOW, config),
                            config.pathPlannerConfig.plannerVisibility.temporaryDestinationView ?
                                    UpdaterPathPlanner.getInstance().getNextVel(TeamColor.YELLOW, yellowRobot.getKey()).get() : null));
                }
            }
        }
    }

    @Override
    public RecordData1 putRecord(@NotNull RecordData1 layer) {
        final double defaultStep =
                ConfigManager.getInstance().getConfig().pathPlannerConfig.withPlannerConfig.defaultStep;
        final UpdaterPathPlanner planner = UpdaterPathPlanner.getInstance();
        for (int id = 0; id < ConfigManager.MAX_ROBOTS; id++) {
            for (TeamColor color : TeamColor.values()) {
                final List<PathSide> pathSide = planner.getResult(color, id).get();
                final double step = planner.getStep(color, id, defaultStep);
                final boolean hasResult = planner.hasResult(color, id);
                final PathPlannerWrapper plannerWrapper = new PathPlannerWrapper(pathSide, step, hasResult);
                layer.setPathPlannerData(color, plannerWrapper, id);
            }
        }
        return layer;
    }
}
