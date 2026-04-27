package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionChaseBall;
import ai_server_cafe.game.action.ActionRobBall;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TestRoleChaser extends AbstractRole {
    protected Map<Integer, ActionChaseBall> chaseBallMap;
    protected Map<Integer, ActionRobBall> robBallMap;

    public TestRoleChaser(TeamColor color, int[] ids) {
        super(color, ids);
        this.chaseBallMap = new HashMap<>();
        this.robBallMap = new HashMap<>();
        for (int id : ids) {
            this.chaseBallMap.put(id, new ActionChaseBall(id, color));
            this.robBallMap.put(id, new ActionRobBall(id, color));
        }
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty()) {
            return list;
        }
        Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(this.color);
        Optional<IntegratedRobot> oppoChaser = MathHelper.nearestRobotToPosition(oppositeRobots, this.world.get().getBall().position());
        List<Integer> candidates = getVisibleIds(this.world.get(), this.color, MathHelper.toArray(this.roleIds));
        if (candidates.isEmpty()) return list;
        for (int id : candidates) {
            FilteredRobot chaser = this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot();
            FilteredBall ball = this.world.get().getBall();
            List<AbstractObstacle> obstacles = oppoChaser.isEmpty() ? CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, id, CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot())
                    : CommonObstacles.getAllExcluded2Robots(this.world.get(), this.color, id, oppoChaser.get().getId(), CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot());
            list.add(new WithPlanner<>(this.robBallMap.get(id), id, this.color, obstacles));
        }
        return list;
    }

    @Override
    public String getName() {
        return "test_chaser";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
