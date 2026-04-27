package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionGoalKeep;
import ai_server_cafe.game.action.ActionHalt;
import ai_server_cafe.game.action.ActionKick;
import ai_server_cafe.game.action.ActionMove;
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
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RolePenaltyKeeper extends AbstractRole {
    private FormationPenaltyKick.attackState state;
    private enum defenseStrategy {
        GOALKEEP,   /// ゴールキープ
        GETBALL,     /// 積極的に取りに行く
    }
    private defenseStrategy strategy;
    // インプレイ経過時間
    private int count;

    public RolePenaltyKeeper(TeamColor color, int[] ids) {
        super(color, ids);
        this.state = FormationPenaltyKick.attackState.WAIT;
        this.strategy = defenseStrategy.GETBALL;
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
        // ロボット中心からキッカー表面までの距離
        final double TO_FACE_RAD = ConfigManager.getInstance().getConfig().toFaceRadius;
        // 10秒
        final int COUNT10 = 600;

        final Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();

        final List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        final Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        final Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);
        final Optional<IntegratedRobot> oppRobot = MathHelper.nearestRobotToPosition(oppositeRobots, ballPos);

        if (MathHelper.robotListFromIds(friendlyRobots, this.roleIds).isEmpty())
            return list;

        final int robotId = this.roleIds.getFirst();
        final IntegratedRobot robot = friendlyRobots.get(robotId);
        final Vector2D robotPos = robot.getRobot().position();

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

        //strategy
        this.strategy = defenseStrategy.GETBALL;
        /*
        // 未使用strategy
        if(oppRobot.isEmpty()) {
            // 敵がいないなら最初にボールを蹴って終了
            this.strategy = defenseStrategy.GETBALL;
        } else if(MathHelper.distancePositionToRobot(new Vector2D(wf.getPenaltyBackX(), 0), oppRobot.get().getRobot()) < 1000) {
            // 敵がゴールに近づいたら出る
            this.strategy = defenseStrategy.GETBALL;
        } else {
            // 敵が遠いならgoalKeepで様子見
            this.strategy = defenseStrategy.GOALKEEP;
        }
        */

        // 処理
        switch (this.state) {
            case WAIT:
                // ゴール中央に移動して相手ゴールを向く
                ActionMove move = this.moveMap.get(robotId);
                move.setPos(new Vector2D(wf.getMinX() + ROBOT_RAD, 0));
                move.setAngle(0);
                if(MathHelper.distance2D(robotPos, wf.getBackPenaltyMark()) < 2000
                        && MathHelper.distance2D(ballPos, wf.getBackPenaltyMark()) < 4*BALL_RAD) {
                    obstacles.add(new ObstacleCircle(ballPos, BALL_RAD, 1000)); // ボールを避ける
                } else {
                    obstacles.add(new ObstacleCircle(ballPos, BALL_RAD, CommonObstacles.getMarginRobot())); // ボールを避ける
                }
                list.add(new WithPlanner<>(move, robotId, this.color, obstacles));
                break;
            case INPLAY:
                switch (this.strategy){
                    case GETBALL :
                        ActionKick kick = this.kickMap.get(robotId);
                        kick.setTarget(wf.getFrontGoalCenter());
                        kick.setIsKeeper(true);
                        kick.kickManually(new Pair<>(EnumKickType.CHIP, 255));
                        kick.setHoldTime(0);
                        list.add(new WithPlanner<>(kick, robotId, this.color, obstacles));
                        this.visualizerTargets = Arrays.asList(ballPos);
                        break;
                    case GOALKEEP:
                        ActionGoalKeep goalKeep = this.goalKeepMap.get(robotId);
                        list.add(new WithPlanner<>(goalKeep, robotId, this.color, obstacles));
                        this.visualizerTargets = new ArrayList<>();
                        break;
                    default:
                        // 例外が起きたら止まる
                        ActionHalt halt = this.haltMap.get(robotId);
                        list.add(new WithPlanner<>(halt, robotId, this.color, obstacles));
                        break;
                }
                this.count++;
                break;
            default:
                break;
        }
        return list;
    }

    @Override
    public String getName() {
        return "role_PenaltyKeeper";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setAttackState(FormationPenaltyKick.attackState state) {
        this.state = state;
    }
}
