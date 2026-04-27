package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterHelpFlag;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FormationSteady extends AbstractFormation {

    private boolean isAttacking;

    public FormationSteady(TeamColor color, int[] ids) {
        super(color, ids);
        this.isAttacking = false;
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty() || this.teamInfo.isEmpty())
            return list;

        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        Vector2D ballPos = world.get().getBall().position();
        Vector2D ballVel = world.get().getBall().velocity();
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);
        int keeper = this.teamInfo.get().getGoalkeeper();

        List<IntegratedRobot> visibleRobots = MathHelper.robotListFromIds(friendlyRobots, visibleIds);
        // ドリブル判定
        {
            Optional<IntegratedRobot> nearestRobot = MathHelper.nearestRobotToPosition(visibleRobots, ballPos);
            if (nearestRobot.isPresent()) {
                UpdaterWorld.getInstance().setHaveBall(
                        MathHelper.distancePositionToRobot(ballPos, nearestRobot.get().getRobot()) < ROBOT_RAD * 2, this.color);
            }
        }

        // 台数の決定
        int numOfDefense = 0;
        int numOfAttacker = 0;
        int numOfSupport = 0;
        {
            Optional<IntegratedRobot> nearestRobot = MathHelper.nearestRobotToPosition(visibleRobots, ballPos);
            Optional<IntegratedRobot> nearestOpp = MathHelper.nearestRobotToPosition(oppositeRobots, ballPos);
            boolean haveBall = nearestRobot.isPresent() && MathHelper.distancePositionToRobot(ballPos, nearestRobot.get().getRobot()) < ROBOT_RAD * 2;
            boolean oppHaveBall = nearestOpp.isPresent() && MathHelper.distancePositionToRobot(ballPos, nearestOpp.get().getRobot()) < ROBOT_RAD * 2;

            // ダブルタッチ対策用
            Optional<Integer> lastKickerId = UpdaterHelpFlag.getInstance().getLastKickerId(this.color).get();
            if (oppHaveBall
                    || (nearestRobot.isPresent() && lastKickerId.isPresent() && nearestRobot.get().getId() != lastKickerId.get()
                    && haveBall)) {
                // 敵がボールに触れた、またはlastKicker以外の味方ロボットがボールに触れたなら
                UpdaterHelpFlag.getInstance().setLastKickerId(Optional.empty(), this.color);
            }

            // 自分たちのボールか
            if (haveBall && !oppHaveBall) {
                this.isAttacking = true;
            } else if (oppHaveBall) {
                this.isAttacking = false;
            }

            if (ballPos.getX() > 0 && visibleIds.size() > 4 && !this.isAttacking) {
                // サポートをつける
                numOfSupport = 1;
            }

            // chaser, keeper, support以外のロボットの数
            int numOfOthers = visibleIds.size() - numOfSupport - 2;

            final double defenceBorderX = -1000;
            if (this.isAttacking) {
                // Attacker優先
                if (ballPos.getX() > defenceBorderX) {
                    numOfAttacker = Math.max(1, numOfOthers * 3 / 4);
                } else {
                    numOfAttacker = Math.max(1, numOfOthers * 3 / 5);
                }
                numOfDefense = numOfOthers - numOfAttacker;
            } else {
                // defense優先
                if (ballPos.getX() > defenceBorderX) {
                    numOfDefense = Math.max(1, numOfOthers / 2);
                } else if (haveBall) {
                    // ボールを取り合っているとき
                    numOfDefense = Math.max(1, numOfOthers * 2 / 3 + 1);
                } else {
                    numOfDefense = numOfOthers;
                }
                numOfAttacker = numOfOthers - numOfDefense;
            }

            // 0以上にする
            numOfDefense = Math.max(0, numOfDefense);
            numOfAttacker = Math.max(0, numOfAttacker);
            numOfSupport = Math.max(0, numOfSupport);
        }

        // ロボットの振り分け
        List<IntegratedRobot> tmpRobots = new ArrayList<>(visibleRobots);
        {
            // キーパー
            tmpRobots.remove(friendlyRobots.get(keeper));
            this.roleKeeper.setRoleIds(new ArrayList<>(Arrays.asList(keeper)));
            list.add(this.roleKeeper);

            // chaser
            Map<IntegratedRobot, Double> chaserScoreMap = new HashMap<>();
            for (IntegratedRobot robot : tmpRobots) {
                Vector2D robotPos = robot.getRobot().position();
                double score = 3000 * Math.exp(-MathHelper.distance2D(robotPos, ballPos) / 1500.0);
                if (ballVel.getNorm() > 1500) {
                    score = Math.exp(-MathHelper.inferiorAngle(robotPos.subtract(ballPos), ballVel))
                            - Math.exp(-ballVel.dotProduct(MathHelper.normalized(robotPos.subtract(ballPos))) / 3000.0)
                            + Math.exp(-MathHelper.distance2D(robotPos, ballPos) / 1500.0);
                }
                // ボールとの間にいる相手ロボットの数
                long count = oppositeRobots.values().stream().filter(InterfaceHelper.getPredicate(new IFuncParam1<Boolean, IntegratedRobot>() {
                    @Override
                    public Boolean function(IntegratedRobot opp) {
                        Vector2D oppPos = opp.getRobot().position();
                        return MathHelper.distance2D(robotPos, oppPos) < MathHelper.distance2D(robotPos, ballPos)
                                && MathHelper.inferiorAngle(ballPos.subtract(robotPos), oppPos.subtract(robotPos)) < 0.2 * Math.PI;
                    }
                })).count();
                score -= 500 * count;

                // ダブルタッチ対策
                Optional<Integer> lastKickerId = UpdaterHelpFlag.getInstance().getLastKickerId(this.color).get();
                if (lastKickerId.isPresent() && lastKickerId.get() == robot.getId()) {
                    score -= 1000000;
                }

                chaserScoreMap.put(robot, score);
            }
            if (chaserScoreMap.isEmpty())
                return list;
            IntegratedRobot chaserRobot = MathHelper.robotWithMaxScore(chaserScoreMap).get();
            tmpRobots.remove(chaserRobot);
            this.roleChaser.setRoleRobots(new ArrayList<>(Arrays.asList(chaserRobot)));
            list.add(this.roleChaser);

            // support
            chaserScoreMap.remove(chaserRobot);
            if (!chaserScoreMap.isEmpty() && numOfSupport != 0) {
                IntegratedRobot supportRobot = MathHelper.robotWithMaxScore(chaserScoreMap).get();
                tmpRobots.remove(supportRobot);
                this.roleSupport.setRoleRobots(List.of(supportRobot));
                list.add(this.roleSupport);
            }

            // defense
            numOfDefense = Math.min(tmpRobots.size(), numOfDefense);
            Map<IntegratedRobot, Double> defenseScoreMap = new HashMap<>();
            for (IntegratedRobot robot : tmpRobots) {
                double score = -1 * robot.getRobot().getX();
                defenseScoreMap.put(robot, score);
            }
            List<IntegratedRobot> defenseRobots = MathHelper.sortInGreaterScore(defenseScoreMap).subList(0, numOfDefense);
            tmpRobots.removeAll(defenseRobots);
            this.roleDefense.setRoleRobots(defenseRobots);
            list.add(this.roleDefense);

            // attack waiter
            this.roleAttackWaiter.setRoleRobots(tmpRobots);
            this.roleAttackWaiter.setNextBallPos(ballVel.getNorm() > 500 ? chaserRobot.getRobot().position() : ballPos);
            list.add(this.roleAttackWaiter);

        }

        return list;
    }

    @Override
    public String getName() {
        return "steady";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}