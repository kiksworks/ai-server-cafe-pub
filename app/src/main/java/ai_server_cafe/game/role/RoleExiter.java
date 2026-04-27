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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleExiter extends AbstractRole {

    public RoleExiter(TeamColor color, int[] ids) {
        super(color, ids);
    }

    @Override
    public List<AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty() || roleIds.isEmpty()) {
            return list;
        }
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);

        final Vector2D ballPos = this.world.get().getBall().position();

        final Field wf = this.world.get().getField();

        List<IntegratedRobot> roleRobots = MathHelper.robotListFromIds(friendlyRobots, this.roleIds);

        List<Vector2D> exitPositionsPositive = new ArrayList<Vector2D>(Arrays.asList(
                new Vector2D(100, wf.getMaxY()),
                new Vector2D(-100, wf.getMaxY()),
                new Vector2D(300, wf.getMaxY()),
                new Vector2D(-300, wf.getMaxY()),
                new Vector2D(500, wf.getMaxY()),
                new Vector2D(-500, wf.getMaxY()),
                new Vector2D(700, wf.getMaxY()),
                new Vector2D(-700, wf.getMaxY()),
                new Vector2D(900, wf.getMaxY()),
                new Vector2D(-900, wf.getMaxY())
        ));

        List<Vector2D> exitPositionsNegative = new ArrayList<Vector2D>(Arrays.asList(
                new Vector2D(100, wf.getMinY()),
                new Vector2D(-100, wf.getMinY()),
                new Vector2D(300, wf.getMinY()),
                new Vector2D(-300, wf.getMinY()),
                new Vector2D(500, wf.getMinY()),
                new Vector2D(-500, wf.getMinY()),
                new Vector2D(700, wf.getMinY()),
                new Vector2D(-700, wf.getMinY()),
                new Vector2D(900, wf.getMinY()),
                new Vector2D(-900, wf.getMinY())
        ));

        List<Vector2D> exitPositions = (this.isInverse() ^ ConfigManager.getInstance().getConfig().exitPositive) ? exitPositionsPositive : exitPositionsNegative;

        List<IntegratedRobot> tmpRobots = new ArrayList<>(roleRobots);

        Map<Integer, Vector2D> targetPositions = new HashMap<Integer, Vector2D>();

        for (Vector2D position : exitPositions) {
            if (tmpRobots.isEmpty()) {
                break;
            }
            IntegratedRobot nearest = MathHelper.nearestRobotToPosition(tmpRobots, position).get();
            targetPositions.put(nearest.getId(), position);
            tmpRobots.remove(nearest);
        }

        for (int id : roleIds) {
            this.moveMap.get(id).setPos(targetPositions.get(id));
            this.moveMap.get(id).setAngle(MathHelper.direction(ballPos, friendlyRobots.get(id).getRobot().position()));
            List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, id, CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot());
            obstacles.add(new ObstacleCircle(ballPos, 500, 100)); // ボールを避ける

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
        return "role_exiter";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

}
