package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionKick;
import ai_server_cafe.game.action.ActionMove;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.ObstacleBox;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.UpdaterPassTarget;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.game.StrategyHelper;
import ai_server_cafe.util.math.KickConverter;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoleKicker extends AbstractRole {
    private boolean isKickOff;
    private boolean isPrepare;
    private Optional<Vector2D> preparePosition;
    private boolean widePenaltyArea;
    private Optional<Vector2D> kickTarget;

    public RoleKicker(TeamColor color, int[] ids) {
        super(color, ids);
        this.isKickOff = false;
        this.isPrepare = true;
        this.preparePosition = Optional.empty();
        this.widePenaltyArea = false;
        this.kickTarget = Optional.empty();
    }

    @Override
    public List<AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (!this.world.isPresent()) {
            return list;
        }
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);

        final Vector2D ballPos = this.world.get().getBall().position();

        Field wf = this.world.get().getField();
        if (MathHelper.robotListFromIds(friendlyRobots, this.roleIds).isEmpty())
            return list;
        int kicker = MathHelper.nearestRobotToPosition(MathHelper.robotListFromIds(friendlyRobots, this.roleIds), ballPos).get().getId();

        IntegratedRobot kickerRobot = friendlyRobots.get(kicker);
        Vector2D robotPos = kickerRobot.getRobot().position();

        List<Integer> otherIds = new ArrayList<>(visibleIds);
        otherIds.remove((Integer) kicker);
        List<IntegratedRobot> waiterRobots = MathHelper.robotListFromIds(friendlyRobots, otherIds);

        List<Vector2D> waiterPositions = new ArrayList<>();
        for (IntegratedRobot robot : waiterRobots) {
            waiterPositions.add(robot.getRobot().position());
        }
        List<Vector2D> kickOffTargets = StrategyHelper.findPassTarget(this.world.get(), this.color, waiterPositions, robotPos);

        List<Vector2D> passTargets = UpdaterPassTarget.getInstance().getPassTargets(this.color).get();

        Vector2D target = new Vector2D(wf.getPenaltyFrontX(), 0); // 有効なパス、シュート目標がみつからないときはゴール正面へ
        List<Vector2D> shootTargets = StrategyHelper.findShootTarget(this.world.get(), this.color, robotPos);
        if (this.isKickOff) {
            if (!shootTargets.isEmpty()) {
                target = StrategyHelper.sortShootTarget(this.world.get(), this.color, shootTargets, robotPos, kickerRobot.getRobot().getTheta()).getFirst();
            } else if (!kickOffTargets.isEmpty()) {
                target = StrategyHelper.sortPassTarget(this.world.get(), this.color, kickOffTargets, robotPos, kickerRobot.getRobot().getTheta()).getFirst();
            }
        } else if (!passTargets.isEmpty()) {
            target = passTargets.getFirst();
        } else if (!shootTargets.isEmpty()) {
            target = StrategyHelper.sortShootTarget(this.world.get(), this.color, shootTargets, robotPos, kickerRobot.getRobot().getTheta()).getFirst();
        }

        if (this.isPrepare) {
            // パスターゲットのロックを解除
            UpdaterPassTarget.getInstance().setTargetLocked(false, this.color);

            // ターゲットを更新
            this.kickTarget = Optional.of(target);

            // ボールの近くで待つ
            ActionMove move = this.moveMap.get(kicker);
            if (this.preparePosition.isPresent()) {
                move.setPos(this.preparePosition.get());
                move.setAngle(MathHelper.direction(ballPos, this.preparePosition.get()));
            } else {
                move.setPos(ballPos.add(MathHelper.normalized(ballPos.subtract(target)).scalarMultiply(800)));
                move.setAngle(MathHelper.direction(target, ballPos));
            }
            List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, move.getId(), CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(move.getId()).getRobot());
            if (this.widePenaltyArea) {
                // ストップゲームのとき
                obstacles.add(new ObstacleBox(new Vector2D(wf.getMaxX(), wf.getPenaltyMinY() - 300), new Vector2D(wf.getPenaltyFrontX() - 300, wf.getPenaltyMaxY() + 300), 100.0));
            }
            obstacles.add(new ObstacleCircle(ballPos, 500, 100)); // ボールを避ける
            list.add(new WithPlanner<>(move, move.getId(), this.color, obstacles));
        } else {
            if (UpdaterPassTarget.getInstance().getPassTargets(this.color).get().isEmpty()) {
                // パスターゲットのロックを解除
                UpdaterPassTarget.getInstance().setTargetLocked(false, this.color);
            } else {
                // パスターゲットをロック
                UpdaterPassTarget.getInstance().setTargetLocked(true, this.color);
            }

            if (this.kickTarget.isEmpty()) {
                this.kickTarget = Optional.of(target);
            }

            // ターゲットを表示
            this.visualizerTargets = List.of(this.kickTarget.get());
            // キック
            ActionKick kick = this.kickMap.get(kicker);
            kick.setIsSetPlay(true);
            kick.kickAutomatically(
                    KickConverter.toPower(kicker, new Pair<>(EnumKickType.STRAIGHT, KickConverter.passSpeed(robotPos, this.kickTarget.get()))),
                    KickConverter.toPower(kicker, new Pair<>(EnumKickType.CHIP, KickConverter.chipSpeed(robotPos, this.kickTarget.get()))));
            kick.setTarget(this.kickTarget.get());
            List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, kick.getId(), CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(kick.getId()).getRobot());
            list.add(new WithPlanner<>(kick, kick.getId(), this.color, obstacles));
        }

        return list;
    }

    @Override
    public String getName() {
        return "role_kick";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    /**
     * キックオフかを設定
     * @param flag キックオフならtrue
     */
    public void setIsKickOff(boolean flag) {
        this.isKickOff = flag;
    }

    /**
     * 蹴らずに待つ
     * @param isPrepare
     */
    public void setIsPrepare(boolean isPrepare) {
        this.isPrepare = isPrepare;
    }

    /**
     * ける前に待つ場所を設定
     * @param position
     */
    public void setPreparePosition(Optional<Vector2D> position) {
        this.preparePosition = position;
    }

    /**
     * ける前に待つ場所を自動で決める
     */
    public void autoPositioning() {
        this.preparePosition = Optional.empty();
    }

    public void setWidePenaltyArea(boolean isWide) {
        this.widePenaltyArea = isWide;
    }
}