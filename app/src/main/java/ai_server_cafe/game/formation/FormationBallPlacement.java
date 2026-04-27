package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FormationBallPlacement extends AbstractFormation {
    private boolean finished;

    public FormationBallPlacement(TeamColor color, int[] ids) {
        super(color, ids);
        this.finished = false;
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty() || this.teamInfo.isEmpty())
            return list;

        Vector2D ballPos = world.get().getBall().position();
        Vector2D ballVel = world.get().getBall().velocity();
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
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
            int numOfOthers = visibleIds.size() - (this.isOurBall ? 2 : 1);

            if (this.isOurBall) {
                // Attacker優先
                if (ballPos.getX() > 0) {
                    numOfAttacker = Math.max(1, numOfOthers / 2);
                } else {
                    numOfAttacker = Math.max(1, numOfOthers / 3);
                }
                numOfDefense = numOfOthers - numOfAttacker;
            } else {
                // defense優先
                if (ballPos.getX() > 0) {
                    numOfDefense = Math.max(1, numOfOthers * 3 / 4);
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
            this.roleKeeper.setBallPlacePos(this.ballPlacePos);
            list.add(this.roleKeeper);

            if (this.isOurBall) {
                // ボール配置係
                Optional<IntegratedRobot> chaserRobot = MathHelper.nearestRobotToPosition(tmpRobots, ballPos);
                if (chaserRobot.isPresent()) {
                    tmpRobots.remove(chaserRobot.get());
                    this.roleBallPlacer.setRoleRobots(new ArrayList<>(Arrays.asList(chaserRobot.get())));
                    this.roleBallPlacer.setBallPlacePos(this.ballPlacePos);
                    list.add(this.roleBallPlacer);
                }
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
            this.roleDefense.setBallPlacePos(this.ballPlacePos);
            list.add(this.roleDefense);

            // waiter
            this.roleAttackWaiter.setRoleRobots(tmpRobots);
            this.roleAttackWaiter.setBallPlacePos(this.ballPlacePos);
            this.roleAttackWaiter.setWidePenaltyArea(true);
            list.add(this.roleAttackWaiter);

        }

        return list;
    }

    @Override
    public String getName() {
        return "ballPlacement";
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}