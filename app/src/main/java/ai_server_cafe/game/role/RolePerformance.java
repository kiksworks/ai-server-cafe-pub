package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionMove;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RolePerformance extends AbstractRole {
    private enum PerformanceState {
        PREPARE,
        ROTATE,
        FINISH
    }
    private PerformanceState state;
    private final Map<Integer, Double> preThetaMap;
    private final Map<Integer, Boolean> isRotateMap;
    private boolean isFinished;
    private int count;

    public RolePerformance(TeamColor color, int[] ids) {
        super(color, ids);
        this.state = PerformanceState.PREPARE;
        this.preThetaMap = new HashMap<>();
        this.isRotateMap = new HashMap<>();
        this.count = 0;
        this.isFinished = false;
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty()) {
            return list;
        }
        // ロボットの半径
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;

        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(this.color);

        if (MathHelper.robotListFromIds(friendlyRobots, this.roleIds).isEmpty())
            return list;

        if(this.isRotateMap.isEmpty() || this.preThetaMap.isEmpty()) {
            for(int id : visibleIds) {
                this.isRotateMap.put(id, Boolean.FALSE);
                this.preThetaMap.put(id, friendlyRobots.get(id).getRobot().getTheta());
            }
        }

        final Map<Integer, Vector2D> waitPositions = makeWaitPositions(this.world.get(), visibleIds);
        if (waitPositions.isEmpty())
            return list;

        // 状態遷移
        switch (this.state) {
            case PREPARE: {
                for(Integer id : visibleIds) {
                    final IntegratedRobot robot = friendlyRobots.get(id);
                    if(this.count > ConfigManager.getInstance().getConfig().performanceWaitFrame) {
                        this.state = PerformanceState.ROTATE;
                    }
                    this.count++;
                    if (MathHelper.distancePositionToRobot(waitPositions.get(id), robot.getRobot()) > 2 * ROBOT_RAD
                            || MathHelper.inferiorAngle(robot.getRobot().getTheta(), FastMath.PI / 36) > FastMath.PI / 18) {
                        this.state = PerformanceState.PREPARE;
                        this.count = 0;
                        break;
                    }
                }
                break;
            }
            case ROTATE : {
                if (this.isRotateMap.containsValue(Boolean.FALSE)) {
                    if(visibleIds.size() > this.roleIds.size()) this.state = PerformanceState.FINISH;
                    for (Integer id : visibleIds) {
                        final double theta = MathHelper.wrap2PI(friendlyRobots.get(id).getRobot().getTheta());
                        if (!this.isRotateMap.containsKey(id) || this.isRotateMap.get(id)) {
                            continue;
                        }
                        if (FastMath.abs(theta - this.preThetaMap.get(id)) > 3.14) {
                            this.isRotateMap.put(id, Boolean.TRUE);
                        } else {
                            this.preThetaMap.put(id, theta);
                        }
                    }
                } else {
                    this.state = PerformanceState.FINISH;
                }
                break;
            }
            case FINISH : {
                this.isFinished = true;
                break;
            }
        }

        // 制御
        switch (this.state) {
            case PREPARE: {
                for(Integer id : visibleIds) {
                    final IntegratedRobot robot = friendlyRobots.get(id);
                    ActionMove move = this.moveMap.get(id);
                    move.setPos(waitPositions.get(id));
                    move.setAngle(FastMath.PI / 36);

                    final List<AbstractObstacle> obstacles =
                            CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, id,
                                    CommonObstacles.getMarginRobot(), robot.getRobot());
                    list.add(new WithPlanner<>(move, id, this.color, obstacles));
                }
                break;
            }
            case ROTATE: {
                final Optional<IntegratedRobot> lowestRobot = friendlyRobots.values().stream().min(
                        InterfaceHelper.getComparator(new IFuncParam1<Double, IntegratedRobot>() {
                            @Override
                            public Double function(IntegratedRobot bot1) {
                                return MathHelper.wrap2PI(bot1.getRobot().getTheta());
                            }
                        }));
                if(lowestRobot.isEmpty()) {
                    this.state = PerformanceState.FINISH;
                } else {
                    final double lowestTheta = lowestRobot.get().getRobot().getTheta();
                    for(Integer id : visibleIds) {
                        final IntegratedRobot robot = friendlyRobots.get(id);
                        ActionMove move = this.moveMap.get(robot.getId());
                        final double theta = robot.getRobot().getTheta();
                        double omega = 6.0;
                        if(theta < -MathHelper.HALF_PI) {
                            omega = 4.0;
                        }
                        if(theta - lowestTheta > FastMath.PI / 18) {
                            omega = omega * FastMath.exp((lowestTheta - theta) * 2);
                        }
                        move.setVelAngular(omega);

                        final List<AbstractObstacle> obstacles =
                                CommonObstacles.getAllExcludedARobot(this.world.get(), this.color,
                                        move.getId(), CommonObstacles.getMarginRobot(), robot.getRobot());
                        list.add(new WithPlanner<>(move, move.getId(), this.color, obstacles));
                    }
                }
                break;
            }
            case FINISH: {
                list.addAll(this.haltMap.values());
                break;
            }
        }
        return list;
    }

    @Override
    public String getName() {
        return "role_performance";
    }

    @Override
    public boolean isFinished() {
        return this.isFinished;
    }

    public void reset() {
        this.state = PerformanceState.PREPARE;
        this.preThetaMap.clear();
        this.isRotateMap.clear();
        this.count = 0;
        this.isFinished = false;
    }

    private Map<Integer, Vector2D> makeWaitPositions(World world, List<Integer> visibleIds) {
        Map<Integer, Vector2D> result = new HashMap<>();
        final Field wf = world.getField();
        final double ROBOT_SPACE = 4 * ConfigManager.getInstance().getConfig().robotRadius;
        final boolean isOddSize = (visibleIds.size() % 2 == 1);
        final int size = visibleIds.size();
        for (int i = 0; i < size; i++) {
            if (wf.getGameHeight() >= 3000.0) {
                if (isOddSize) {
                    // 真ん中がロボット
                    result.put(visibleIds.get(i), new Vector2D(wf.getPenaltyBackX() + 2500,
                            ROBOT_SPACE * (i - (double) ((size - 1) / 2))));
                } else {
                    // 真ん中がゴールライン
                    result.put(visibleIds.get(i), new Vector2D(wf.getPenaltyBackX() + 2500,
                            ROBOT_SPACE * (i - (double) (size / 2))));
                }
            } else {
                if (isOddSize) {
                    // 真ん中がロボット
                    result.put(visibleIds.get(i), new Vector2D(0.0, ROBOT_SPACE * (i - (double) ((size - 1) / 2))));
                } else {
                    // 真ん中がゴールライン
                    result.put(visibleIds.get(i), new Vector2D(0.0, ROBOT_SPACE * (i - (double) (size / 2))));
                }
            }
        }
        return result;
    }
}
