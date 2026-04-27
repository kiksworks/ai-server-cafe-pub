package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleWaiter extends AbstractRole {
    private static final Logger l = LogManager.getLogger("roleWaiter");
    private List<Vector2D> waitPositions;
    private Map<Integer, Vector2D> waitPositionsWithId;
    private boolean isSetWithId;
    private boolean avoidBall;

    public RoleWaiter(TeamColor color, int[] ids) {
        super(color, ids);
        this.waitPositions = new ArrayList<>();
        this.waitPositionsWithId = new HashMap<>();
        this.isSetWithId = false;
        this.avoidBall = false;
    }

    public void setWaitPositionss(List<Vector2D> positions) {
        this.waitPositions = positions;
        this.isSetWithId = false;
    }

    public void setWaitPositionsWithId(Map<Integer, Vector2D> positions) {
        this.waitPositionsWithId = positions;
        this.isSetWithId = true;
    }

    @Override
    public List<AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (!this.world.isPresent() || roleIds.isEmpty()) {
            return list;
        }
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);

        final Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();

        final Field wf = this.world.get().getField();
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;

        List<IntegratedRobot> roleRobots = MathHelper.robotListFromIds(friendlyRobots, roleIds);

        List<IntegratedRobot> tmpRobots = new ArrayList<>(roleRobots);

        Map<Integer, Vector2D> targetPositions = new HashMap<Integer, Vector2D>();

        if (isSetWithId) {
            targetPositions = waitPositionsWithId;
        } else {
            for (Vector2D position : waitPositions) {
                if (tmpRobots.isEmpty()) {
                    break;
                }
                IntegratedRobot nearest = MathHelper.nearestRobotToPosition(tmpRobots, position).get();
                targetPositions.put(nearest.getId(), position);
                tmpRobots.remove(nearest);
            }
        }

        for (int id : roleIds) {
            if (!targetPositions.containsKey(id)) {
                l.info("positions aren't enough");
                list.add(this.haltMap.get(id));
                break;
            }
            this.moveMap.get(id).setPos(targetPositions.get(id));
            this.moveMap.get(id).setAngle(MathHelper.direction(ballPos, friendlyRobots.get(id).getRobot().position()));
            List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, id, CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot());
            if (this.avoidBall) {
                obstacles.add(new ObstacleCircle(this.world.get().getBall().position(), 500.0, CommonObstacles.getMarginRobot()));
            }
            if (this.moveMap.get(id).isFinished()) {
                list.add(this.haltMap.get(id));
            } else {
                list.add(new WithPlanner<>(this.moveMap.get(id), id, this.color, obstacles));
            }
        }

        return list;
    }

    @Override
    public String getName() {
        return "role_waiter";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setAvoidBall(boolean flag) {
        this.avoidBall = flag;
    }
}