package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionChaseBall;
import ai_server_cafe.game.action.ActionCircularMove;
import ai_server_cafe.game.action.ActionRobBall;
import ai_server_cafe.game.action.ActionTestMove;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.model.game.RobotTask;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoleTAction extends AbstractRole {
    protected final Map<Integer, RobotTask> taskMap;
    protected Map<Integer, ActionChaseBall> chaseBallMap;
    protected Map<Integer, ActionRobBall> robBallMap;
    protected Map<Integer, ActionCircularMove> circularMoveMap;
    protected Map<Integer, ActionTestMove> testMoveMap;

    public RoleTAction(TeamColor color, int[] ids) {
        super(color, ids);
        this.chaseBallMap = new HashMap<>();
        this.robBallMap = new HashMap<>();
        this.taskMap = new HashMap<>();
        this.circularMoveMap = new HashMap<>();
        this.testMoveMap = new HashMap<>();
        for (int id : ids) {
            this.chaseBallMap.put(id, new ActionChaseBall(id, color));
            this.robBallMap.put(id, new ActionRobBall(id, color));
            this.circularMoveMap.put(id, new ActionCircularMove(id, color));
            this.testMoveMap.put(id, new ActionTestMove(id, color));
        }
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty()) {
            return list;
        }
        Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(this.color);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(this.color);
        Optional<IntegratedRobot> oppoChaser = MathHelper.nearestRobotToPosition(oppositeRobots, this.world.get().getBall().position());
        List<Integer> candidates = getVisibleIds(this.world.get(), this.color, MathHelper.toArray(this.roleIds));
        if (candidates.isEmpty()) return list;
        FilteredBall ball = this.world.get().getBall();
        for (int id : candidates) {
            RobotTask task = this.taskMap.get(id);
            List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, id, CommonObstacles.getMarginRobot(), friendlyRobots.get(id).getRobot());
            if (task.taskName.equals("move")) {
                if (task.target.isBall()) {
                    this.moveMap.get(id).setPos(ball.position());
                } else if (task.target.getRobot().isPresent()) {
                    Map<Integer, IntegratedRobot> targetRobots = task.target.getRobot().get().getFirst() == this.color ? friendlyRobots : oppositeRobots;
                    int targetId = task.target.getRobot().get().getSecond();
                    if (targetRobots.containsKey(targetId)) {
                        this.moveMap.get(id).setPos(targetRobots.get(targetId).getRobot().position());
                    }
                } else if (task.target.getPos().isPresent()) {
                    this.moveMap.get(id).setPos(MathHelper.getHead2(task.target.getPos().get()));
                    this.moveMap.get(id).setAngle(task.target.getPos().get().getZ());
                } else {
                    this.moveMap.get(id).setVel(MathHelper.getHead2(task.target.getVel()));
                    this.moveMap.get(id).setVelAngular(task.target.getVel().getZ());
                }
                list.add(new WithPlanner<>(this.moveMap.get(id), id, this.color, obstacles).setAllowMove(!task.isFake));
            }
            else if (task.taskName.equals("tMove")) {
                if (task.target.isBall()) {
                    this.testMoveMap.get(id).setPos(ball.position());
                } else if (task.target.getRobot().isPresent()) {
                    Map<Integer, IntegratedRobot> targetRobots = task.target.getRobot().get().getFirst() == this.color ? friendlyRobots : oppositeRobots;
                    int targetId = task.target.getRobot().get().getSecond();
                    if (targetRobots.containsKey(targetId)) {
                        this.testMoveMap.get(id).setPos(targetRobots.get(targetId).getRobot().position());
                    }
                } else if (task.target.getPos().isPresent()) {
                    this.testMoveMap.get(id).setPos(MathHelper.getHead2(task.target.getPos().get()));
                    this.testMoveMap.get(id).setAngle(task.target.getPos().get().getZ());
                } else {
                    this.testMoveMap.get(id).setVel(MathHelper.getHead2(task.target.getVel()));
                    this.testMoveMap.get(id).setVelAngular(task.target.getVel().getZ());
                }
                list.add(new WithPlanner<>(this.testMoveMap.get(id), id, this.color, obstacles).setAllowMove(!task.isFake));
            }
            else if (task.taskName.equals("circularMove")) {
                Optional<Double> d = task.target.getDouble();
                double r = 0.0;
                if (d.isPresent()) {
                    r = d.get();
                }
                this.circularMoveMap.get(id).setRadius(r);
                list.add(this.circularMoveMap.get(id));
            }
            // TODO implement other actions

        }
        return list;
    }

    public void setTaskMap(Map<Integer, RobotTask> tasks) {
        this.taskMap.putAll(tasks);
    }

    @Override
    public String getName() {
        return "role_test_action";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
