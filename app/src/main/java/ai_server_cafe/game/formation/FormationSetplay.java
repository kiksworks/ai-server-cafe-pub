package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterHelpFlag;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FormationSetplay extends AbstractFormation {
    private boolean finished;
    private double startTime;

    public FormationSetplay(TeamColor color, int[] ids) {
        super(color, ids);
        this.finished = false;
        this.startTime = TimeHelper.now();
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty() || this.teamInfo.isEmpty())
            return list;

        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        Vector2D ballPos = world.get().getBall().position();
        Vector2D ballVel = world.get().getBall().velocity();
        final Field wf = this.world.get().getField();
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);
        int keeper = this.teamInfo.get().getGoalkeeper();

        List<IntegratedRobot> visibleRobots = MathHelper.robotListFromIds(friendlyRobots, visibleIds);

        // 終了判定
        if (ballVel.getNorm() > 200) {
            this.finished = true;
        }

        // 台数の決定
        int numOfDefense = 0;
        int numOfAttacker = 0;
        {
            // chaser, keeper以外のロボットの数
            int numOfOthers = visibleIds.size() - 2;

            final double defenceBorderX = -1000;
            if (this.isOurBall) {
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
                } else {
                    numOfDefense = numOfOthers;
                }
                numOfAttacker = numOfOthers - numOfDefense;
            }

            // 0以上にする
            numOfDefense = Math.max(0, numOfDefense);
            numOfAttacker = Math.max(0, numOfAttacker);
        }

        // ロボットの振り分け
        List<IntegratedRobot> tmpRobots = new ArrayList<>(visibleRobots);
        {
            // キーパー
            tmpRobots.remove(friendlyRobots.get(keeper));
            this.roleKeeper.setRoleIds(new ArrayList<>(Arrays.asList(keeper)));
            this.roleKeeper.setAvoidBall(true);
            list.add(this.roleKeeper);

            // kicker
            Map<IntegratedRobot, Double> kickerScoreMap = new HashMap<>();
            for (IntegratedRobot robot : tmpRobots) {
                double score = ballVel.dotProduct(robot.getRobot().position().subtract(ballPos)) - 2000 * MathHelper.distance2D(robot.getRobot().position(), ballPos);
                kickerScoreMap.put(robot, score);
            }
            if (kickerScoreMap.isEmpty())
                return list;
            IntegratedRobot kickerRobot = MathHelper.robotWithMaxScore(kickerScoreMap).get();
            tmpRobots.remove(kickerRobot);
            this.roleKicker.setRoleRobots(new ArrayList<>(Arrays.asList(kickerRobot)));
            // 相手のセットプレーかつ5秒未満なら蹴らない
            this.roleKicker.setIsPrepare(!this.isOurBall && TimeHelper.now() < this.startTime + 5);
            this.roleKicker.setPreparePosition(Optional.of(ballPos.add(MathHelper.normalized(wf.getBackGoalCenter().subtract(ballPos)).scalarMultiply(700.0))));
            list.add(this.roleKicker);

            if (MathHelper.distancePositionToRobot(ballPos, kickerRobot.getRobot()) < ROBOT_RAD * 2) {
                // ダブルタッチ対策のため、Updaterに登録
                UpdaterHelpFlag.getInstance().setLastKickerId(Optional.of(kickerRobot.getId()), this.color);
            }

            // defense
            numOfDefense = Math.min(numOfDefense, tmpRobots.size());
            Map<IntegratedRobot, Double> defenseScoreMap = new HashMap<>();
            for (IntegratedRobot robot : tmpRobots) {
                double score = -1 * robot.getRobot().getX();
                defenseScoreMap.put(robot, score);
            }
            List<IntegratedRobot> defenseRobots = MathHelper.sortInGreaterScore(defenseScoreMap).subList(0, numOfDefense);
            tmpRobots.removeAll(defenseRobots);
            this.roleDefense.setRoleRobots(defenseRobots);
            this.roleDefense.setAvoidBall(true);
            list.add(this.roleDefense);

            // waiter
            this.roleAttackWaiter.setRoleRobots(tmpRobots);
            this.roleAttackWaiter.setWidePenaltyArea(true);
            this.roleAttackWaiter.setAvoidBall(true);
            list.add(this.roleAttackWaiter);

        }

        return list;
    }

    @Override
    public String getName() {
        return "setplay";
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}