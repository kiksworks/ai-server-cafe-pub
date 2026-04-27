package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionBallPlace;
import ai_server_cafe.game.action.ActionGetBall;
import ai_server_cafe.game.action.ActionGoalKeep;
import ai_server_cafe.game.action.ActionGuard;
import ai_server_cafe.game.action.ActionHalt;
import ai_server_cafe.game.action.ActionHold;
import ai_server_cafe.game.action.ActionKick;
import ai_server_cafe.game.action.ActionMitoma;
import ai_server_cafe.game.action.ActionMove;
import ai_server_cafe.game.action.ActionProtectBall;
import ai_server_cafe.game.action.ActionReceive;
import ai_server_cafe.game.action.ActionRobBall;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.util.TeamColor;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractRole {
    protected final TeamColor color;
    protected final int[] activeRobots;
    protected List<Integer> roleIds;
    private boolean inverse;
    protected Optional<World> world;
    protected Optional<GcRefereeMessage.Referee.TeamInfo> teamInfo;
    protected long now;

    protected Map<Integer, ActionGetBall> getBallMap;
    protected Map<Integer, ActionGoalKeep> goalKeepMap;
    protected Map<Integer, ActionHalt> haltMap;
    protected Map<Integer, ActionKick> kickMap;
    protected Map<Integer, ActionMove> moveMap;
    protected Map<Integer, ActionReceive> receiveMap;
    protected Map<Integer, ActionBallPlace> ballPlaceMap;
    protected Map<Integer, ActionGuard> guardMap;
    protected Map<Integer, ActionProtectBall> protectBallMap;
    protected Map<Integer, ActionMitoma> mitomaMap;
    protected Map<Integer, ActionRobBall> robBallMap;
    protected Map<Integer, ActionHold> holdMap;

    protected List<Vector2D> visualizerTargets;
    protected List<Pair<Vector2D, Vector2D>> visualizerBoxes;

    public AbstractRole (TeamColor color, int[] ids) {
        this.world = Optional.empty();
        this.teamInfo = Optional.empty();
        this.color = color;
        this.activeRobots = ids;
        this.roleIds = new ArrayList<>();
        this.inverse = false;

        this.getBallMap = new HashMap<>();
        this.goalKeepMap = new HashMap<>();
        this.haltMap = new HashMap<>();
        this.kickMap = new HashMap<>();
        this.moveMap = new HashMap<>();
        this.receiveMap = new HashMap<>();
        this.ballPlaceMap = new HashMap<>();
        this.guardMap = new HashMap<>();
        this.protectBallMap = new HashMap<>();
        this.mitomaMap = new HashMap<>();
        this.robBallMap = new HashMap<>();
        this.holdMap = new HashMap<>();
        for (int id : ids) {
            this.getBallMap.put(id, new ActionGetBall(id, color, Vector2D.ZERO));
            this.goalKeepMap.put(id, new ActionGoalKeep(id, color));
            this.haltMap.put(id, new ActionHalt(id, color));
            this.kickMap.put(id, new ActionKick(id, color));
            this.moveMap.put(id, new ActionMove(id, color));
            this.receiveMap.put(id, new ActionReceive(id, color));
            this.ballPlaceMap.put(id, new ActionBallPlace(id, color, Vector2D.ZERO));
            this.guardMap.put(id, new ActionGuard(id, color));
            this.protectBallMap.put(id, new ActionProtectBall(id, color));
            this.mitomaMap.put(id, new ActionMitoma(id, color));
            this.robBallMap.put(id, new ActionRobBall(id, color));
            this.holdMap.put(id, new ActionHold(id, color));
        }

        this.visualizerTargets = new ArrayList<>();
        this.visualizerBoxes = new ArrayList<>();
    }

    public void setRoleIds(List<Integer> ids) {
        this.roleIds = ids;
    }

    public void setRoleRobots(List<IntegratedRobot> robots) {
        List<Integer> ids = new ArrayList<>();
        for (IntegratedRobot robot : robots) {
            ids.add(robot.getId());
        }
        this.roleIds = ids;
    }

    public void setInverse(boolean value) {
        this.inverse = value;
    }

    public void setWorld(World world) {
        this.world = Optional.of(world);
    }

    public List<? extends AbstractAction> update(long now) {
        this.now = now;
        List<? extends AbstractAction> result = this.execute();
        for (AbstractAction aa : result) {
            if (this.world.isPresent()) {
                aa.setWorld(this.world.get());
            }
            aa.setInvert(this.inverse);
        }
        return result;
    }

    protected abstract List<? extends AbstractAction> execute();

    public abstract String getName();

    public abstract boolean isFinished();

    public static List<Integer> getVisibleIds(World world, TeamColor color, int[] activeRobots) {
        List<Integer> result = new ArrayList<>();
        for (int id : activeRobots) {
            if (world.getFriendlyRobotMap(color).containsKey(id)) {
                result.add(id);
            }
        }
        return result;
    }

    public List<Vector2D> getVisualizerTargets() {
        return this.visualizerTargets;
    }

    public List<Pair<Vector2D, Vector2D>> getVisualizerBoxes() {
        return this.visualizerBoxes;
    }

    protected boolean isInverse() {
        return this.inverse;
    }
}