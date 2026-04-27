package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FormationStop extends AbstractFormation {

    public FormationStop(TeamColor color, int[] ids) {
        super(color, ids);
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

        // 台数の決定
        int numOfDefense = 0;
        int numOfAttacker = 0;
        int numOfExiter = Math.max(visibleIds.size() - this.teamInfo.get().getMaxAllowedBots(), 0);
        {
            Optional<IntegratedRobot> nearestRobot = MathHelper.nearestRobotToPosition(visibleRobots, ballPos);
            Optional<IntegratedRobot> nearestOpp = MathHelper.nearestRobotToPosition(oppositeRobots, ballPos);

            // chaser, keeper, exiter以外のロボットの数
            int numOfOthers = Math.max(visibleIds.size() - 2 - numOfExiter, 0);

            if (ballPos.getX() > -1000) {
                numOfAttacker = Math.max(1, numOfOthers / 2);
            } else {
                numOfAttacker = Math.max(1, numOfOthers / 3);
            }
            numOfDefense = numOfOthers - numOfAttacker;

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
                if (MathHelper.toList(ConfigManager.getInstance().getConfig().newDribblerIds).contains(robot.getId()) ) {
                    // 新型ドリブラーで蹴り出さないように
                    score -= 100000000;
                }
                kickerScoreMap.put(robot, score);
            }
            if (kickerScoreMap.isEmpty())
                return list;
            IntegratedRobot kickerRobot = MathHelper.robotWithMaxScore(kickerScoreMap).get();
            tmpRobots.remove(kickerRobot);
            this.roleKicker.setRoleRobots(new ArrayList<>(Arrays.asList(kickerRobot)));
            this.roleKicker.setIsPrepare(true);
            this.roleKicker.setPreparePosition(Optional.of(ballPos.add(MathHelper.normalized(wf.getBackGoalCenter().subtract(ballPos)).scalarMultiply(700.0))));
            this.roleKicker.setWidePenaltyArea(true);
            list.add(this.roleKicker);

            // exiter
            Map<IntegratedRobot, Double> exitScoreMap = new HashMap<>();
            numOfExiter = Math.min(numOfExiter, tmpRobots.size());
            for (IntegratedRobot robot : tmpRobots) {
                double score = -1 * MathHelper.distancePositionToRobot(
                        new Vector2D(0, this.isInverse() ^ ConfigManager.getInstance().getConfig().exitPositive ? wf.getMaxY() : wf.getMinY()),
                        robot.getRobot());
                exitScoreMap.put(robot, score);
            }
            List<IntegratedRobot> exitRobots = MathHelper.sortInGreaterScore(exitScoreMap).subList(0, numOfExiter);
            tmpRobots.removeAll(exitRobots);
            this.roleExiter.setRoleRobots(exitRobots);
            list.add(this.roleExiter);

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
            if (ballPos.getNorm() < 500) {
                this.roleKickoffWaiter.setRoleRobots(tmpRobots);
                this.roleKickoffWaiter.setAvoidBall(true);
                list.add(this.roleKickoffWaiter);
            } else {
                this.roleAttackWaiter.setRoleRobots(tmpRobots);
                this.roleAttackWaiter.setAvoidBall(true);
                this.roleAttackWaiter.setWidePenaltyArea(true);
                list.add(this.roleAttackWaiter);
            }


        }

        return list;
    }

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}