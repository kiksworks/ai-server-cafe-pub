package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionHalt;
import ai_server_cafe.game.action.ActionKick;
import ai_server_cafe.game.action.ActionMitoma;
import ai_server_cafe.game.action.ActionMove;
import ai_server_cafe.game.action.ActionProtectBall;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.game.formation.FormationPenaltyKick;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.game.StrategyHelper;
import ai_server_cafe.util.math.KickConverter;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RolePenaltyKicker extends AbstractRole {
    private FormationPenaltyKick.attackState state;
    private enum attackStrategy {
        KEEP,    /// 敵を引き付ける
        CHIP,    /// チップキック
        KICK,    /// キック
        AVOID,   /// 敵を避ける
        DRIBBLE, /// ドリブルしながら様子見
        NULL     /// 作戦なし(ゴールに向かってキック)
    }
    // strategy
    private attackStrategy strategy;
    // インプレイ経過時間
    private int count;

    public RolePenaltyKicker(TeamColor color, int[] ids) {
        super(color, ids);
        this.state = FormationPenaltyKick.attackState.WAIT;
        this.strategy = attackStrategy.DRIBBLE;
        this.count = 0;
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty()) {
            return list;
        }
        final Field wf = this.world.get().getField();
        // ロボット半径
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        // ボール半径
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;

        // 相手がペナルティエリア外にいるとき
        // 相手ペナルティーエリアからこの距離以内の位置からシュートを打つ
        final double SHOOT_AREA_MARGIN1 = 2500;

        // 相手がペナルティエリア内にいるとき
        // 相手ペナルティーエリアからこの距離以内の位置からシュートを打つ
        final double SHOOT_AREA_MARGIN2 = 2000;


        // 相手の速度閾値
        final double CHIP_VEL_THRESHOLD = 1200;
        // 相手がこの距離以内になったらチップ
        final double CHIP_MARGIN = 2000;
        // 10秒
        final int COUNT10 = 600;

        final Vector2D ballPos = this.world.get().getBall().position();

        final List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        final Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        final Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);
        final Optional<IntegratedRobot> oppRobot = MathHelper.nearestRobotToPosition(oppositeRobots, ballPos);

        if (MathHelper.robotListFromIds(friendlyRobots, this.roleIds).isEmpty())
            return list;

        final int robotId = this.roleIds.getFirst();
        final IntegratedRobot robot = friendlyRobots.get(robotId);
        final Vector2D robotPos = robot.getRobot().position();
        final double direction = robot.getRobot().getTheta();

        // obstacles
        List<AbstractObstacle> obstacles =
                CommonObstacles.getFriendlyRobotsIdsExcluded(this.world.get(), this.color,
                        new int[]{robotId}, robot.getRobot(), CommonObstacles.getMarginRobot());
        obstacles.addAll(CommonObstacles.getFriendlyGoal(wf, CommonObstacles.getMarginRobot()));
        obstacles.addAll(CommonObstacles.getOppositeGoal(wf, CommonObstacles.getMarginRobot()));
        obstacles.add(CommonObstacles.getOppositePenaltyArea(wf, CommonObstacles.getMarginRobot()));
        if(oppRobot.isPresent()) {
            obstacles.addAll(CommonObstacles.getOppositeRobots(this.world.get(), this.color,
                    oppRobot.get().getRobot(), CommonObstacles.getMarginRobot()));
        }

        // strategy
        Vector2D shootTarget = Vector2D.ZERO;
        Vector2D dribbleTarget = Vector2D.ZERO;
        Vector2D avoidTarget = Vector2D.ZERO;
        double kickPower = 5000.0;
        double holdTime = 0;
        final List<Vector2D> shootTargets = StrategyHelper.findShootTarget(this.world.get(), this.color, robotPos);
        final List<Vector2D> dribbleTargets = StrategyHelper.findDribbleTarget(this.world.get(), this.color, robotPos);
        final List<Vector2D> avoidTargets = oppRobot.isPresent() ?
                StrategyHelper.findAvoidTarget(this.world.get(), this.color, robot, oppRobot.get()) : new ArrayList<>();
        if(!shootTargets.isEmpty() && oppRobot.isPresent()
                && robotPos.getX() > wf.getPenaltyFrontX() - SHOOT_AREA_MARGIN1
                && oppRobot.get().getRobot().position().getX() < wf.getPenaltyFrontX() - ROBOT_RAD) {
            // 敵がいる and 敵がペナルティエリア外
            // シュート
            this.strategy = attackStrategy.KICK;
            List<Vector2D> sortedTargets =
                    StrategyHelper.sortShootTarget(this.world.get(), this.color, shootTargets, robotPos, direction);
            shootTarget = sortedTargets.getFirst();
            holdTime = 0;
            this.visualizerTargets = sortedTargets;
        } else if(!shootTargets.isEmpty() && (oppRobot.isPresent()
                && robotPos.getX() > wf.getPenaltyFrontX() - SHOOT_AREA_MARGIN2
                && oppRobot.get().getRobot().position().getX() > wf.getPenaltyFrontX() - ROBOT_RAD)
                || oppRobot.isEmpty()) {
            // (敵がいる and 敵がペナルティエリア内) or 敵がいないとき
            // シュート
            this.strategy = attackStrategy.KICK;
            List<Vector2D> sortedTargets =
                    StrategyHelper.sortShootTarget(this.world.get(), this.color, shootTargets, robotPos, direction);
            shootTarget = sortedTargets.getFirst();
            holdTime = 10;
            this.visualizerTargets = sortedTargets;
        } else if(!shootTargets.isEmpty() && this.count > 0.7*COUNT10) {
            // 7秒以上経過
            this.strategy = attackStrategy.KICK;
            List<Vector2D> sortedTargets =
                    StrategyHelper.sortShootTarget(this.world.get(), this.color, shootTargets, robotPos, direction);
            shootTarget = sortedTargets.getFirst();
            holdTime = 0;
            this.visualizerTargets = sortedTargets;
        } else if(robotPos.getX() < 0 && oppRobot.get().getRobot().velocity().getNorm() > CHIP_VEL_THRESHOLD
                && MathHelper.distancePositionToRobot(ballPos, oppRobot.get().getRobot()) < CHIP_MARGIN) {
            // 敵がいる and 敵が詰めてる and 敵ボール間距離が近い and チップしても場外しないとき
            // チップ
            this.strategy = attackStrategy.CHIP;
            holdTime = 0;
            // divisionAだと5000.0かも?
            kickPower = 4500.0;
            this.visualizerTargets = Arrays.asList(wf.getFrontGoalCenter());
        } else if(robotPos.getX() < 0 && oppRobot.get().getRobot().velocity().getNorm() > CHIP_VEL_THRESHOLD
                && MathHelper.distancePositionToRobot(ballPos, oppRobot.get().getRobot()) > CHIP_MARGIN) {
            // 敵がいる and 敵が詰めてる and 敵ボール間距離が遠い and チップしても場外しないとき
            // キープ
            this.strategy = attackStrategy.KEEP;
            this.visualizerTargets = Arrays.asList(ballPos);
        } else if(oppRobot.get().getRobot().position().getX() > wf.getPenaltyFrontX() - ROBOT_RAD
                && robotPos.getX() < 0 && oppRobot.get().getRobot().velocity().getNorm() < CHIP_VEL_THRESHOLD) {
            // 敵がペナルティエリア内 and 敵が詰めてない and 蹴ってもペナルティエリアに入らないとき
            // キックして近づく
            this.strategy = attackStrategy.KICK;
            holdTime = 0;
            kickPower = Math.sqrt(2 * 160 * MathHelper.distance2D(ballPos, wf.getFrontGoalCenter()));
            shootTarget = wf.getFrontGoalCenter();
            this.visualizerTargets = Arrays.asList(wf.getFrontPenaltyMark());
        } else if (!dribbleTargets.isEmpty()
                && MathHelper.distancePositionToRobot(ballPos, oppRobot.get().getRobot()) > CHIP_MARGIN) {
            // 敵ボール間距離が遠い
            // ドリブル
            this.strategy =attackStrategy.DRIBBLE;
            List<Vector2D> sortedTargets =
                    StrategyHelper.sortDribbleTarget(this.world.get(), this.color, dribbleTargets, robotPos, direction);
            dribbleTarget = sortedTargets.getFirst();
            this.visualizerTargets = sortedTargets;
        } else if (!avoidTargets.isEmpty()
                && MathHelper.distancePositionToRobot(ballPos, oppRobot.get().getRobot()) < CHIP_MARGIN) {
            // 敵がいる and 敵ボール間距離が近いとき
            // 回避
            kickPower = Math.sqrt(2 * 900 * MathHelper.distance2D(ballPos, avoidTarget));
            this.strategy = attackStrategy.AVOID;
            List<Vector2D> sortedTargets =
                    StrategyHelper.sortAvoidTarget(this.world.get(), this.color, avoidTargets, robot, oppRobot.get());
            avoidTarget = sortedTargets.getFirst();
            holdTime = 5;
            this.visualizerTargets = sortedTargets;
        } else {
            // 上記以外なら敵ゴールに向かってシュート
            this.strategy = attackStrategy.NULL;
            holdTime = 10;
            this.visualizerTargets = new ArrayList<>();
        }

        // 処理
        switch (this.state) {
            case FormationPenaltyKick.attackState.WAIT:
                //ペナルティマーク付近に移動して相手ゴールを向く
                ActionMove move = this.moveMap.get(robotId);
                move.setPos(wf.getBackPenaltyMark().subtract(new Vector2D(300+BALL_RAD+ROBOT_RAD, 0)));
                move.setAngle(0);
                list.add(move);
                if(MathHelper.distance2D(robotPos, wf.getBackPenaltyMark()) < 3*ROBOT_RAD) {
                    obstacles.add(new ObstacleCircle(ballPos, BALL_RAD, 100));
                } else {
                    obstacles.add(new ObstacleCircle(ballPos, BALL_RAD, 400));
                }
                list.add(new WithPlanner<>(move, robotId, this.color, obstacles));
                break;
            case FormationPenaltyKick.attackState.INPLAY:
                switch (this.strategy) {
                    case CHIP:
                        ActionKick chipKick = this.kickMap.get(robotId);
                        chipKick.setTarget(wf.getFrontGoalCenter());
                        chipKick.setHoldTime(holdTime);
                        chipKick.kickManually(new Pair<>(EnumKickType.CHIP,
                                KickConverter.toPower(robotId, new Pair<>(EnumKickType.CHIP, kickPower))));
                        list.add(new WithPlanner<>(chipKick, robotId, this.color, obstacles));
                        this.visualizerTargets = Arrays.asList(wf.getFrontGoalCenter());
                        break;
                    case KEEP:
                        ActionProtectBall protectBall = this.protectBallMap.get(robotId);
                        list.add(new WithPlanner<>(protectBall, robotId, this.color, obstacles));
                        this.visualizerTargets = new ArrayList<>();
                        break;
                    case DRIBBLE:
                        ActionMitoma mitoma = this.mitomaMap.get(robotId);
                        mitoma.setTarget(dribbleTarget);
                        list.add(new WithPlanner<>(mitoma, robotId, this.color, obstacles));
                        break;
                    case KICK:
                        ActionKick shootKick = this.kickMap.get(robotId);
                        shootKick.setTarget(shootTarget);
                        shootKick.kickManually(new Pair<>(EnumKickType.STRAIGHT,
                                KickConverter.toPower(robotId, new Pair<>(EnumKickType.STRAIGHT, kickPower))));
                        shootKick.setHoldTime(holdTime);
                        list.add(new WithPlanner<>(shootKick, robotId, this.color, obstacles));
                        break;
                    case AVOID:
                        ActionKick avoidKick = this.kickMap.get(robotId);
                        avoidKick.setTarget(avoidTarget);
                        avoidKick.kickManually(new Pair<>(EnumKickType.STRAIGHT,
                                KickConverter.toPower(robotId, new Pair<>(EnumKickType.STRAIGHT, kickPower))));
                        avoidKick.setHoldTime(holdTime);
                        list.add(new WithPlanner<>(avoidKick, robotId, this.color, obstacles));
                        break;
                    default:
                        // NULLの時の処理
                        ActionKick kick = this.kickMap.get(robotId);
                        kick.setTarget(wf.getFrontGoalCenter());
                        kick.setHoldTime(holdTime);
                        kick.kickManually(KickConverter.toPower(robotId, new Pair<>(EnumKickType.STRAIGHT,5000.0)));
                        list.add(new WithPlanner<>(kick, robotId, this.color, obstacles));
                        break;
                }
                this.count++;
                break;
            default:
                // 例外が起きたら止まる
                ActionHalt halt = this.haltMap.get(robotId);
                list.add(new WithPlanner<>(halt, robotId, this.color, obstacles));
                break;
        }
        return list;
    }

    @Override
    public String getName() {
        return "role_PenaltyKicker";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setAttackState(FormationPenaltyKick.attackState state) {
        this.state = state;
    }
}
