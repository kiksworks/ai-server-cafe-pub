package ai_server_cafe.gui.item;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.VisionArea;
import ai_server_cafe.gui.interfaces.IContainerCafe;
import ai_server_cafe.gui.interfaces.IGraphicalComponent;
import ai_server_cafe.gui.item.basic.BallCafe;
import ai_server_cafe.gui.item.basic.CircleCafe;
import ai_server_cafe.gui.item.basic.LineCafe;
import ai_server_cafe.gui.item.basic.NoneCafe;
import ai_server_cafe.gui.item.basic.StringCafe;
import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.model.field.World;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.records.ver1.WorldWrapper;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.gui.ColorHelper;
import ai_server_cafe.util.interfaces.SerializableJson;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.LinkedHashMap;

public class DrawBall implements IItemDraw {
    @Override
    public void init(Class<? extends IContainerCafe> area,
            LinkedHashMap<String, IGraphicalComponent> graphicalComponents) {
        if (area != VisionArea.class) return;
        graphicalComponents.putLast("ball", new NoneCafe());
        graphicalComponents.putLast("ball_speed", new NoneCafe());
        graphicalComponents.putLast("ball_velocity_line", new NoneCafe());
        graphicalComponents.putLast("blue_dribble_start_pos", new NoneCafe());
        graphicalComponents.putLast("yellow_dribble_start_pos", new NoneCafe());
    }

    @Override
    public void update(Class<? extends IContainerCafe> area,
            LinkedHashMap<String, IGraphicalComponent> graphicalComponents, Config config) {
        if (area != VisionArea.class) {
            return;
        }
        boolean worldInvert = config.visibility.invertVisible;
        World world = UpdaterWorld.getInstance().getWorld(worldInvert);
        graphicalComponents.put("ball", new BallCafe(world.getBall(), ColorHelper.BALL_ORANGE));
        if (config.visibility.ballVelocityVisible) {
            FilteredBall ball = world.getBall();
            double ballVel = FastMath.sqrt(ball.getVx() * ball.getVx() + ball.getVy() * ball.getVy());
            // ball速度の色と大きさ
            Color ballVelColor = Color.LIGHT_GRAY;
            int labelSize = 170;
            if (ballVel > 6500.0) {
                labelSize += 20;
                ballVelColor = Color.RED;
            }
            graphicalComponents.put("ball_speed",
                    new StringCafe(ballVelColor, (int) ball.getX() - 50, (int) ball.getY() - 230,
                            String.valueOf((int) ballVel), labelSize));
            graphicalComponents.put("ball_velocity_line",
                    new LineCafe(ball.getX(), ball.getY(), ball.getX() + ball.getVx(), ball.getY() + ball.getVy(),
                            ballVelColor, 10.0F));
        }
        // ドリブル開始地点の表示
        final Vector2D bluePos = world.getDribbleStartPos(TeamColor.BLUE);
        final Vector2D yellowPos = world.getDribbleStartPos(TeamColor.YELLOW);
        final double dribbleMargin = 1000;
        if (world.getHaveBall(TeamColor.BLUE)) {
            graphicalComponents.put("blue_dribble_start_pos",
                    new CircleCafe(ColorHelper.ROBOT_BLUE, bluePos.getX(), bluePos.getY(), dribbleMargin, false,
                            30.0F));
        }
        if (world.getHaveBall(TeamColor.YELLOW)) {
            graphicalComponents.put("yellow_dribble_start_pos",
                    new CircleCafe(ColorHelper.ROBOT_YELLOW, yellowPos.getX(), yellowPos.getY(), dribbleMargin, false,
                            30.0F));
        }
    }

    @Override
    public RecordData1 putRecord(@NotNull RecordData1 layer) {
        World world = SerializableJson.deserializeJson(layer.worldData);
        if (world == null) {
            world = new World();
        }
        WorldWrapper worldWrapper = new WorldWrapper(world);
        worldWrapper.ball = UpdaterWorld.getInstance().getWorld(false).getBall();
        worldWrapper.blueDribbleStartPos =
                UpdaterWorld.getInstance().getWorld(false).getDribbleStartPos(TeamColor.BLUE);
        worldWrapper.yellowDribbleStartPos =
                UpdaterWorld.getInstance().getWorld(false).getDribbleStartPos(TeamColor.YELLOW);
        worldWrapper.blueHaveBall = UpdaterWorld.getInstance().getWorld(false).getHaveBall(TeamColor.BLUE);
        worldWrapper.yellowHaveBall = UpdaterWorld.getInstance().getWorld(false).getHaveBall(TeamColor.YELLOW);
        world = worldWrapper.getFixed();
        layer.worldData = SerializableJson.serializeJson(world);
        return layer;
    }
}
