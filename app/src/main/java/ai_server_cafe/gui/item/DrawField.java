package ai_server_cafe.gui.item;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.VisionArea;
import ai_server_cafe.gui.interfaces.IContainerCafe;
import ai_server_cafe.gui.interfaces.IGraphicalComponent;
import ai_server_cafe.gui.item.basic.CircleCafe;
import ai_server_cafe.gui.item.basic.LineCafe;
import ai_server_cafe.gui.item.basic.NoneCafe;
import ai_server_cafe.gui.item.basic.RectCafe;
import ai_server_cafe.gui.item.basic.StringCafe;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.World;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.records.ver1.WorldWrapper;
import ai_server_cafe.replay.ReplayState;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterReplay;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.gui.ColorHelper;
import ai_server_cafe.util.interfaces.SerializableJson;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.LinkedHashMap;

public class DrawField implements IItemDraw {

    private double lastTime = 0L;

    @Override
    public void init(Class<? extends IContainerCafe> area,
            LinkedHashMap<String, IGraphicalComponent> graphicalComponents) {
        if (area == VisionArea.class) {
            graphicalComponents.putLast("background", new NoneCafe());
            graphicalComponents.putLast("goalToGoalLine", new NoneCafe());
            graphicalComponents.putLast("centerLine", new NoneCafe());
            graphicalComponents.putLast("fieldLine", new NoneCafe());
            graphicalComponents.putLast("outsideLine", new NoneCafe());
            graphicalComponents.putLast("centerCircle", new NoneCafe());
            graphicalComponents.putLast("ourPenalty", new NoneCafe());
            graphicalComponents.putLast("oppositePenalty", new NoneCafe());
            graphicalComponents.putLast("ourGoal", new NoneCafe());
            graphicalComponents.putLast("oppositeGoal", new NoneCafe());
            graphicalComponents.putLast("labelMinX", new NoneCafe());
            graphicalComponents.putLast("labelMaxX", new NoneCafe());
        }
    }

    @Override
    public void update(Class<? extends IContainerCafe> area,
            LinkedHashMap<String, IGraphicalComponent> graphicalComponents, Config config) {
        if (area == VisionArea.class) {
            boolean worldInvert = config.visibility.invertVisible;
            Field field = UpdaterWorld.getInstance().getField();
            graphicalComponents.put("background",
                    new RectCafe(-field.getCarpetWidth() / 2.0, -field.getCarpetHeight() / 2.0, field.getCarpetWidth(),
                            field.getCarpetHeight(), ColorHelper.FIELD_DARK));
            graphicalComponents.put("goalToGoalLine",
                    new LineCafe(-field.getGameWidth() / 2.0, 0.0, field.getGameWidth() / 2.0, 0.0,
                            ColorHelper.LINE_WHITE, 12.0F));
            graphicalComponents.put("centerLine",
                    new LineCafe(0.0, -field.getGameHeight() / 2.0, 0.0, field.getGameHeight() / 2.0,
                            ColorHelper.LINE_WHITE, 12.0F));
            graphicalComponents.put("fieldLine",
                    new RectCafe(-field.getGameWidth() / 2.0, -field.getGameHeight() / 2.0, field.getGameWidth(),
                            field.getGameHeight(), ColorHelper.LINE_WHITE, 12.0F));
            graphicalComponents.put("outsideLine",
                    new RectCafe(-field.getFieldWidth() / 2.0, -field.getFieldHeight() / 2.0, field.getFieldWidth(),
                            field.getFieldHeight(), ColorHelper.WALL_RED, 12.0F));
            graphicalComponents.put("centerCircle",
                    new CircleCafe(ColorHelper.LINE_WHITE, 0.0, 0.0, field.getCenterCircle(), false, 12.0F));
            graphicalComponents.put("ourPenalty",
                    new RectCafe(-field.getGameWidth() / 2.0, -field.getPenaltyWidth() / 2.0, field.getPenaltyLength(),
                            field.getPenaltyWidth(), ColorHelper.LINE_WHITE, 12.0F));
            graphicalComponents.put("oppositePenalty",
                    new RectCafe(field.getGameWidth() / 2.0 - field.getPenaltyLength(), -field.getPenaltyWidth() / 2.0,
                            field.getPenaltyLength(), field.getPenaltyWidth(), ColorHelper.LINE_WHITE, 12.0F));

            boolean isColored = config.visibility.teamGoalColorVisible;
            if (isColored) {
                if (config.isGoalOfYellowPositive) {
                    graphicalComponents.put("ourGoal", new RectCafe(-field.getGameWidth() / 2.0 - field.getGoalLength(),
                            -field.getGoalWidth() / 2.0, field.getGoalLength(), field.getGoalWidth(),
                            worldInvert ? ColorHelper.LINE_YELLOW : ColorHelper.LINE_BLUE, 12.0F));

                    graphicalComponents.put("oppositeGoal",
                            new RectCafe(field.getGameWidth() / 2.0, -field.getGoalWidth() / 2.0, field.getGoalLength(),
                                    field.getGoalWidth(), worldInvert ? ColorHelper.LINE_BLUE : ColorHelper.LINE_YELLOW,
                                    12.0F));
                } else {
                    graphicalComponents.put("ourGoal", new RectCafe(-field.getGameWidth() / 2.0 - field.getGoalLength(),
                            -field.getGoalWidth() / 2.0, field.getGoalLength(), field.getGoalWidth(),
                            worldInvert ? ColorHelper.LINE_BLUE : ColorHelper.LINE_YELLOW, 12.0F));
                    graphicalComponents.put("oppositeGoal",
                            new RectCafe(field.getGameWidth() / 2.0, -field.getGoalWidth() / 2.0, field.getGoalLength(),
                                    field.getGoalWidth(), worldInvert ? ColorHelper.LINE_YELLOW : ColorHelper.LINE_BLUE,
                                    12.0F));
                }
            } else {
                // デフォルトの色
                graphicalComponents.put("ourGoal",
                        new RectCafe(-field.getGameWidth() / 2.0 - field.getGoalLength(), -field.getGoalWidth() / 2.0,
                                field.getGoalLength(), field.getGoalWidth(), ColorHelper.LINE_WHITE, 12.0F));
                graphicalComponents.put("oppositeGoal",
                        new RectCafe(field.getGameWidth() / 2.0, -field.getGoalWidth() / 2.0, field.getGoalLength(),
                                field.getGoalWidth(), ColorHelper.LINE_WHITE, 12.0F));
            }

            if(config.enableRecord && ConfigManager.getInstance().isStart()) {
                if(TimeHelper.now() - this.lastTime > config.getCycleTime()) {
                    this.lastTime = TimeHelper.now();
                }
                final Color recordRed = new Color(255, 0, 0,
                        (int) Math.clamp(60 * FastMath.sin(6.28 * this.lastTime) + 60, 10, 100));
                graphicalComponents.put("record_field_rect", new RectCafe(
                        -field.getCarpetWidth() / 2 + 32, -field.getCarpetHeight() / 2 + 32,
                        field.getCarpetWidth() - 32, field.getCarpetHeight() - 32,
                        Color.RED, 32.0F));
                final int recordX = (int) (field.getMaxX() + 4 * field.getGameMargin());
                final int recordY = (int) (field.getMaxY() + field.getGameMargin());
                graphicalComponents.put("record_string", new StringCafe(
                        Color.GRAY, recordX, recordY, "REC.", 300));
                graphicalComponents.put("record_circle", new CircleCafe(recordRed,
                        recordX - field.getGameMargin() / 2, recordY + field.getGameMargin() / 2,
                        100.0, true, 0F));
                graphicalComponents.put("record_rect", new RectCafe(
                        recordX - field.getGoalLength() - 100, recordY - 60,
                        1100, 370, recordRed, 32.0F));
            } else if(UpdaterReplay.getInstance().isPresent()) {
                if(TimeHelper.now() - this.lastTime > config.getCycleTime()) {
                    this.lastTime = TimeHelper.now();
                }
                final Color replayRed = new Color(255, 0, 0,
                        UpdaterReplay.getInstance().getReplayState().equals(ReplayState.REPLAY) ?
                                (int) Math.clamp(60 * FastMath.sin(6.28 * this.lastTime) + 60, 10, 100)
                                : 100);
                graphicalComponents.put("replay_field_rect", new RectCafe(
                        -field.getCarpetWidth() / 2 + 32, -field.getCarpetHeight() / 2 + 32,
                        field.getCarpetWidth() - 32, field.getCarpetHeight() - 32,
                        Color.RED, 32.0F));
                final int replayX = (int) (field.getMaxX() + 4 * field.getGameMargin()) - 160;
                final int replayY = (int) (field.getMaxY() + field.getGameMargin());
                graphicalComponents.put("replay_string", new StringCafe(
                        Color.GRAY, replayX, replayY, "REPLAY", 300));
                graphicalComponents.put("replay_circle", new CircleCafe(replayRed,
                        replayX - field.getGameMargin() / 2, replayY + field.getGameMargin() / 2,
                        100.0, true, 0F));
                graphicalComponents.put("replay_rect", new RectCafe(
                        replayX - field.getGoalLength() - 100, replayY - 60,
                        1600, 370, replayRed, 32.0F));

            }

            graphicalComponents.put("labelMinX",
                    new StringCafe(ColorHelper.LINE_WHITE, -800, (int) field.getMaxX() + 400,
                            worldInvert ? "Positive Side" : "Negative Side", 250, true, MathHelper.HALF_PI));
            graphicalComponents.put("labelMaxX",
                    new StringCafe(ColorHelper.LINE_WHITE, -800, (int) field.getMinX() - 600,
                            worldInvert ? "Negative Side" : "Positive Side", 250, true, MathHelper.HALF_PI));
        }
    }

    @Override
    public RecordData1 putRecord(@Nonnull RecordData1 frameData) {
        World world = SerializableJson.deserializeJson(frameData.worldData);
        if (world == null) {
            world = new World();
        }
        WorldWrapper worldWrapper = new WorldWrapper(world);
        worldWrapper.field = UpdaterWorld.getInstance().getField();
        world = worldWrapper.getFixed();
        frameData.worldData = SerializableJson.serializeJson(world);
        return frameData;
    }
}
