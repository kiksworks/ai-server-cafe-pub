package ai_server_cafe.records.ver1;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.WrapperCloneableMap;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
/**
 * ReplayとRecordDataのsetterおよびgetterとして使用するクラス
 */
public class WorldWrapper {
    public Map<Integer, IntegratedRobot> blueRobotMap;
    public Map<Integer, IntegratedRobot> yellowRobotMap;
    public FilteredBall ball;
    public Field field;
    public Vector2D blueDribbleStartPos;
    public Vector2D yellowDribbleStartPos;
    public boolean blueHaveBall;
    public boolean yellowHaveBall;

    public WorldWrapper() {
        this.blueRobotMap = new HashMap<>();
        this.yellowRobotMap = new HashMap<>();
        this.ball = new FilteredBall();
        this.field = new Field();
        this.blueDribbleStartPos = Vector2D.ZERO;
        this.yellowDribbleStartPos = Vector2D.ZERO;
        this.blueHaveBall = false;
        this.yellowHaveBall = false;
    }

    public WorldWrapper(@Nonnull World world) {
        this.blueRobotMap = world.getFriendlyRobotMap(TeamColor.BLUE);
        this.yellowRobotMap = world.getFriendlyRobotMap(TeamColor.YELLOW);
        this.ball = world.getBall();
        this.field = world.getField();
        this.blueDribbleStartPos = world.getDribbleStartPos(TeamColor.BLUE);
        this.yellowDribbleStartPos = world.getDribbleStartPos(TeamColor.YELLOW);
        this.blueHaveBall = world.getHaveBall(TeamColor.BLUE);
        this.yellowHaveBall = world.getHaveBall(TeamColor.YELLOW);
    }

    public World getFixed() {
        return new World(new WrapperCloneableMap<>(blueRobotMap), new WrapperCloneableMap<>(yellowRobotMap), ball,
                field, blueDribbleStartPos, yellowDribbleStartPos, blueHaveBall, yellowHaveBall);
    }
}
