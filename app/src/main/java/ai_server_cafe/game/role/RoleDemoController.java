package ai_server_cafe.game.role;

import ai_server_cafe.device.joystick.StatusJoyStick;
import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionChaseBall;
import ai_server_cafe.game.action.ActionController;
import ai_server_cafe.game.action.ActionDemoController;
import ai_server_cafe.game.action.ActionDribble;
import ai_server_cafe.game.action.ActionHold;
import ai_server_cafe.game.action.ActionKick;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterJoyStick;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.game.StrategyHelper;
import ai_server_cafe.util.math.KickConverter;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleDemoController extends AbstractRole {
    private Map<Integer, ActionController> actionControllerMap;
    private Map<Integer, ActionDemoController> actionDemoControllerMap;
    private Map<Integer, ActionChaseBall> actionChaseBallMap;
    private Map<Integer, ActionDribble> actionDribbleMap;

    private String lastActionName;

    public RoleDemoController(TeamColor color, int[] ids) {
        super(color, ids);
        this.actionControllerMap = new HashMap<>();
        this.actionDemoControllerMap = new HashMap<>();
        this.actionChaseBallMap = new HashMap<>();
        this.actionDribbleMap = new HashMap<>();
        this.lastActionName = "";
        for (int id : ids) {
            this.actionControllerMap.put(id, new ActionController(id, color));
            this.actionDemoControllerMap.put(id, new ActionDemoController(id, color));
            this.actionChaseBallMap.put(id, new ActionChaseBall(id, color));
            this.actionDribbleMap.put(id, new ActionDribble(id, color));
        }
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty()) {
            return list;
        }
        List<StatusJoyStick> states = UpdaterJoyStick.getInstance().update().get();
        final Field wf = this.world.get().getField();
        final FilteredBall ball = this.world.get().getBall();
        final Vector2D frontGoal = world.get().getField().getFrontGoalCenter();
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final Vector2D ballPos = ball.position();

        // シュート速度
        final double SHOOT_SPEED = 3500;
        final double CHIP_SPEED = 2000;

        for (int id : this.roleIds) {
            final FilteredRobot robot = this.world.get().getFriendlyRobotMap(color).get(id).getRobot();
            AbstractAction action;
            if (!states.isEmpty()) {
                StatusJoyStick controller;
                if (states.size() > 1) {
                    // コントローラーが2台あるとき
                    if (this.color.isYellow()) {
                        // 黄色チームは1台目のコントローラーを使用
                        controller = states.getFirst().clone();
                    } else {
                        // 青色チームは2台目のコントローラーを使用
                        controller = states.get(1).clone();
                    }
                } else {
                    // コントローラーが1台のとき
                    controller = states.getFirst().clone();
                }

                states.removeFirst();
                if ((controller.buttonB || controller.buttonY)
                        && (Math.abs(ballPos.getX()) > wf.getMaxX() - ROBOT_RAD || Math.abs(ballPos.getY()) > wf.getMaxY() - ROBOT_RAD)) {
                    // プレースメント
                    ballPlaceMap.get(id).setAbpTarget(Vector2D.ZERO);
                    action = ballPlaceMap.get(id);
                } else if (controller.buttonB) {
                    // 自動攻撃
                    if (MathHelper.distance2D(frontGoal, robot.position()) > 2500) {
                        actionDribbleMap.get(id).setTarget(frontGoal.scalarMultiply(0.9));
                        action = actionDribbleMap.get(id);
                    } else {
                        List<Vector2D> shootTargets = StrategyHelper.findShootTarget(this.world.get(), this.color,
                                robot.position());
                        Vector2D target = frontGoal;
                        if (!shootTargets.isEmpty()) {
                            target = StrategyHelper
                                    .sortShootTarget(this.world.get(), this.color, shootTargets, robot.position(),
                                            robot.getTheta())
                                    .getFirst();
                        }
                        kickMap.get(id).setTarget(target);
                        kickMap.get(id).kickManually(new Pair<>(EnumKickType.STRAIGHT,
                                KickConverter.toPower(id, new Pair<>(EnumKickType.STRAIGHT, SHOOT_SPEED))));
                        action = kickMap.get(id);
                    }
                } else if (controller.buttonY) {
                    // ボールを持って、ゴールの方を向く
                    if (controller.leftButton) {
                        ActionKick kick = kickMap.get(id);
                        kick.setTarget(wf.getFrontGoalCenter());
                        kick.setHoldTime(0);
                        kick.kickManually(new Pair<>(EnumKickType.STRAIGHT, KickConverter.toPower(
                                id, new Pair<>(EnumKickType.STRAIGHT, SHOOT_SPEED))));
                        action = kick;
                    } else if (controller.rightButton) {
                        ActionKick kick = kickMap.get(id);
                        kick.setTarget(wf.getFrontGoalCenter());
                        kick.setHoldTime(0);
                        kick.kickManually(new Pair<>(EnumKickType.CHIP, KickConverter.toPower(
                                id, new Pair<>(EnumKickType.CHIP, CHIP_SPEED))));
                        action = kick;
                    } else {
                        ActionHold actionHold = holdMap.get(id);
                        actionHold.setTarget(wf.getFrontGoalCenter());
                        action = actionHold;
                    }
                } else if (controller.buttonA) {
                    // 防御
                    goalKeepMap.get(id).setChipPow(0);
                    action = goalKeepMap.get(id);
                } else {
                    this.actionDemoControllerMap.get(id).setKickPow(
                            KickConverter.toPower(id, new Pair<>(EnumKickType.STRAIGHT, SHOOT_SPEED)),
                            KickConverter.toPower(
                                    id, new Pair<>(EnumKickType.CHIP, CHIP_SPEED)));
                    if (!lastActionName.equals(this.actionDemoControllerMap.get(id).getName()))
                        // action変更時にロボットの方向を伝える
                        this.actionDemoControllerMap.get(id).setTargetTheta(robot.getTheta());
                    this.actionDemoControllerMap.get(id).setControllerStatus(controller);
                    action = this.actionDemoControllerMap.get(id);
                }
                list.add(action);
                lastActionName = action.getName();
            } else {
                list.add(this.haltMap.get(id));
            }
        }
        if (list.isEmpty())
            return list;
        List<AbstractAction> returnList = new ArrayList<>();
        for (AbstractAction action : list) {
            List<AbstractObstacle> obstacles = new ArrayList<>();
            obstacles.addAll(CommonObstacles.getFriendlyRobotsIdsExcluded(this.world.get(), this.color,
                    new int[]{action.getId()}, this.world.get().getFriendlyRobotMap(this.color).get(action.getId()).getRobot(), CommonObstacles.getMarginRobot()));
            obstacles.addAll(CommonObstacles.getOppositeRobots(this.world.get(), this.color,
                    this.world.get().getFriendlyRobotMap(this.color).get(action.getId()).getRobot(), CommonObstacles.getMarginRobot()));
            //obstacles.addAll(CommonObstacles.getFieldOutSideLinesDemo(wf, ROBOT_RAD));
            returnList.add(new WithPlanner<>(action, action.getId(), this.color, obstacles));
        }
        return returnList;
    }

    @Override
    public String getName() {
        return "controller";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
