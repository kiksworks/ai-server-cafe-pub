package ai_server_cafe.game.formation;

import ai_server_cafe.config.Config;
import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FormationPenaltyKick extends AbstractFormation {
    private enum penaltyMode {
        ATTACK,   ///< 攻撃側
        DEFENSE   ///< 守備側
    }
    private penaltyMode mode;
    public enum attackState {
        WAIT,    ///< ペナルティマークに移動
        INPLAY,  ///< インプレイ中の処理
    }
    private attackState state;

    public FormationPenaltyKick(TeamColor color, int[] ids) {
        super(color, ids);
        this.mode = penaltyMode.ATTACK;
        this.state = attackState.WAIT;
    }

    @Override
    protected List<? extends AbstractRole> execute() {
        List<AbstractRole> list = new ArrayList<>();
        if (this.world.isEmpty() || this.teamInfo.isEmpty())
            return list;

        final Field wf = this.world.get().getField();
        Config config = ConfigManager.getInstance().getConfig();
        final double ROBOT_RAD = config.robotRadius;
        final double BALL_RAD = config.ballRadius;
        final Vector2D ballPos = world.get().getBall().position();
        final Vector2D ballVel = world.get().getBall().velocity();
        final List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        if(visibleIds.isEmpty())
            return list;
        final Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(this.color);

        // pkに使うロボットIDの指定
        final int pkTmpId = config.pk.isAssignedRobotId ? config.pk.assignedRobotId : this.teamInfo.get().getGoalkeeper();
        final int pkRobotId = visibleIds.contains(pkTmpId) ? pkTmpId : visibleIds.getFirst();

        List<IntegratedRobot> visibleRobots = MathHelper.robotListFromIds(friendlyRobots, visibleIds);

        // ドリブル判定
        {
            Optional<IntegratedRobot> nearestRobot = MathHelper.nearestRobotToPosition(visibleRobots, ballPos);
            if (nearestRobot.isPresent()) {
                UpdaterWorld.getInstance().setHaveBall(
                        MathHelper.distancePositionToRobot(ballPos, nearestRobot.get().getRobot()) < ROBOT_RAD * 2, this.color);
            }
        }

        this.mode = this.isOurBall ? penaltyMode.ATTACK : penaltyMode.DEFENSE;
        // PREPARE_PENALTYが送信されるとtrue、されないとnullを受け取る
        this.state = this.isPrepare ? attackState.WAIT : attackState.INPLAY;
        // ロボットの振り分け
        List<IntegratedRobot> tmpRobots = new ArrayList<>(visibleRobots);
        switch (this.mode) {
            case penaltyMode.ATTACK:
                // kicker
                tmpRobots.remove(friendlyRobots.get(pkRobotId));
                this.rolePenaltyKicker.setRoleIds(new ArrayList<>(Arrays.asList(pkRobotId)));
                this.rolePenaltyKicker.setAttackState(this.state);
                list.add(this.rolePenaltyKicker);
                break;
            case penaltyMode.DEFENSE :
                // keeper
                tmpRobots.remove(friendlyRobots.get(pkRobotId));
                this.rolePenaltyKeeper.setRoleIds(new ArrayList<>(Arrays.asList(pkRobotId)));
                this.rolePenaltyKeeper.setAttackState(this.state);
                list.add(this.rolePenaltyKeeper);
                break;
            default:
                break;
        }
        if(tmpRobots.isEmpty()) return list;

        // waiter
        List<Vector2D> positions = this.waitPositions(this.world.get(), this.color, this.mode);
        this.roleWaiter.setRoleRobots(tmpRobots);
        this.roleWaiter.setWaitPositionss(positions);
        this.roleWaiter.setAvoidBall(true);
        list.add(this.roleWaiter);
        return list;
    }

    @Override
    public String getName() {
        return "PenaltyKick";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    /**
     *
     * @param world world
     * @param color TeamColor
     * @param mode  攻撃側か守備側か
     * @return 自陣ロボットの待機位置リストを返す
     */
    private List<Vector2D> waitPositions(World world, TeamColor color, penaltyMode mode) {
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final Field wf = world.getField();
        List<Vector2D> positionsA = new ArrayList<>();
        List<Vector2D> positionsB = new ArrayList<>();
        if(mode == penaltyMode.ATTACK) {
            for (int id : this.activeRobots) {
                // ロボットを2列に並ばせる
                positionsA.add(new Vector2D(wf.getMinX() + 3*(id%2)*ROBOT_RAD,
                        wf.getMinY() + 3*(id/2)*ROBOT_RAD).scalarMultiply(0.95));
                positionsB.add(new Vector2D(wf.getMinX() - 3*(id%2)*ROBOT_RAD,
                        wf.getMaxY() - 3*(id/2)*ROBOT_RAD).scalarMultiply(0.95));
            }
            return this.isCollideRobots(world, color, positionsA) ?
                    positionsB : positionsA;
        } else {
            for(int id : this.activeRobots) {
                // ロボットを2列に並ばせる
                positionsA.add(new Vector2D(wf.getMaxX() + 3*(id%2)*ROBOT_RAD,
                        wf.getMaxY() - 3*(id/2)*ROBOT_RAD).scalarMultiply(0.95));
                positionsB.add(new Vector2D(wf.getMaxX() - 3*(id%2)*ROBOT_RAD,
                        wf.getMinY() + 3*(id/2)*ROBOT_RAD).scalarMultiply(0.95));
            }
            return this.isCollideRobots(world, color, positionsB) ?
                    positionsA : positionsB;
        }
    }

    /**
     *
     * @param world     world
     * @param color     TeamColor
     * @param positions 待機位置候補
     * @return 既に敵ロボットがそこに待機していたらtrue
     */
    private boolean isCollideRobots(World world, TeamColor color, List<Vector2D> positions) {
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        if(positions.isEmpty() || world.getOppositeRobotMap(color).isEmpty())
            return false;
        for (IntegratedRobot robot : world.getOppositeRobotMap(color).values().stream().toList()) {
            if(MathHelper.isCollidedWithBox(positions.getFirst(), positions.getLast(), robot.getRobot().position(), 4*ROBOT_RAD)) {
                // ロボットが一台でも近くにいたらtrue
                return true;
            }
        }
        return false;
    }
}
