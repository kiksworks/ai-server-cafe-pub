package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.ObstacleBox;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.model.field.obstacle.base.ObstacleSegment;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterPassTarget;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoleAttackWaiter extends AbstractRole {

    private boolean avoidBall;
    private boolean widePenaltyArea;
    private Optional<Vector2D> ballPlacePos;
    private Optional<Vector2D> nextBallPos;

    public RoleAttackWaiter(TeamColor color, int[] ids) {
        super(color, ids);
        this.avoidBall = false;
        this.widePenaltyArea = false;
        this.ballPlacePos = Optional.empty();
        this.nextBallPos = Optional.empty();
    }

    @Override
    public List<AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (!this.world.isPresent() || roleIds.isEmpty()) {
            return list;
        }
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);

        final Vector2D ballPos = this.nextBallPos.orElse(this.world.get().getBall().position());
        final Vector2D ballVel = this.world.get().getBall().velocity();

        final Field wf = this.world.get().getField();
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;

        List<IntegratedRobot> roleRobots = MathHelper.robotListFromIds(friendlyRobots, this.roleIds);

        List<Pair<Vector2D, Vector2D>> waiterBoxList = new ArrayList<>();
        if (ballPos.getX() > wf.getMaxX() / 3) {
            waiterBoxList.add(new Pair<>(new Vector2D(wf.getPenaltyFrontX(), 0.5 * wf.getPenaltyMinY()), new Vector2D(wf.getPenaltyFrontX() - 1000, 0.5 * wf.getPenaltyMaxY())));
            waiterBoxList.add(new Pair<>(new Vector2D(wf.getPenaltyFrontX(), 1.5 * wf.getPenaltyMinY()), new Vector2D(wf.getPenaltyFrontX() - 1000, 0.5 * wf.getPenaltyMinY())));
            waiterBoxList.add(new Pair<>(new Vector2D(wf.getPenaltyFrontX(), 0.5 * wf.getPenaltyMaxY()), new Vector2D(wf.getPenaltyFrontX() - 1000, 1.5 * wf.getPenaltyMaxY())));
            waiterBoxList.add(new Pair<>(new Vector2D(wf.getMaxX(), wf.getPenaltyMinY() - 1000), new Vector2D(wf.getPenaltyFrontX(), wf.getPenaltyMinY())));
            waiterBoxList.add(new Pair<>(new Vector2D(wf.getMaxX(), wf.getPenaltyMaxY()), new Vector2D(wf.getPenaltyFrontX(), wf.getPenaltyMaxY() + 1000)));
            waiterBoxList.add(new Pair<>(new Vector2D(0.2 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() - 1000, 0.4 * (wf.getMinY() - ballPos.getY()) + ballPos.getY() - 1000), new Vector2D(0.2 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() + 1000, 0.4 * (wf.getMinY() - ballPos.getY()) + ballPos.getY() + 1000)));
            waiterBoxList.add(new Pair<>(new Vector2D(0.2 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() - 1000, 0.4 * (wf.getMaxY() - ballPos.getY()) + ballPos.getY() - 1000), new Vector2D(0.2 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() + 1000, 0.4 * (wf.getMaxY() - ballPos.getY()) + ballPos.getY() + 1000)));
        } else if (ballPos.getX() > wf.getMinX() / 3) {
            waiterBoxList.add(new Pair<>(new Vector2D(0.5 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() - 1000, 0.5 * (wf.getMinY() - ballPos.getY()) + ballPos.getY() - 1000), new Vector2D(0.5 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() + 1000, 0.5 * (wf.getMinY() - ballPos.getY()) + ballPos.getY() + 1000)));
            waiterBoxList.add(new Pair<>(new Vector2D(0.5 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() - 1000, 0.5 * (wf.getMaxY() - ballPos.getY()) + ballPos.getY() - 1000), new Vector2D(0.5 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() + 1000, 0.5 * (wf.getMaxY() - ballPos.getY()) + ballPos.getY() + 1000)));
            waiterBoxList.add(new Pair<>(new Vector2D(0.5 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() - 1000, 0.5 * (0 - ballPos.getY()) + ballPos.getY() - 1000), new Vector2D(0.5 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() + 1000, 0.5 * (0 - ballPos.getY()) + ballPos.getY() + 1000)));
            waiterBoxList.add(new Pair<>(new Vector2D(0.2 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() - 1000, 0.4 * (wf.getMinY() - ballPos.getY()) + ballPos.getY() - 1000), new Vector2D(0.2 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() + 1000, 0.4 * (wf.getMinY() - ballPos.getY()) + ballPos.getY() + 1000)));
            waiterBoxList.add(new Pair<>(new Vector2D(0.2 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() - 1000, 0.4 * (wf.getMaxY() - ballPos.getY()) + ballPos.getY() - 1000), new Vector2D(0.2 * (wf.getMaxX() - ballPos.getX()) + ballPos.getX() + 1000, 0.4 * (wf.getMaxY() - ballPos.getY()) + ballPos.getY() + 1000)));
            waiterBoxList.add(new Pair<>(new Vector2D(0, 0.3 * wf.getMinY()), new Vector2D(-2000, 0.3 * wf.getMaxY())));
            waiterBoxList.add(new Pair<>(new Vector2D(0, 0.7 * wf.getMaxY()), new Vector2D(-2000, 0.3 * wf.getMaxY())));
        } else {
            waiterBoxList.add(new Pair<>(new Vector2D(2000, -2000), new Vector2D(0, 0)));
            waiterBoxList.add(new Pair<>(new Vector2D(2000, 0), new Vector2D(0, 2000)));
            waiterBoxList.add(new Pair<>(new Vector2D(0, 0.7 * wf.getMinY()), new Vector2D(-2000, 0.3 * wf.getMinY())));
            waiterBoxList.add(new Pair<>(new Vector2D(0, 0.3 * wf.getMinY()), new Vector2D(-2000, 0.3 * wf.getMaxY())));
            waiterBoxList.add(new Pair<>(new Vector2D(0, 0.7 * wf.getMaxY()), new Vector2D(-2000, 0.3 * wf.getMaxY())));
            waiterBoxList.add(new Pair<>(new Vector2D(-2000, 0.7 * wf.getMinY()), new Vector2D(-4000, 0.3 * wf.getMinY())));
            waiterBoxList.add(new Pair<>(new Vector2D(-2000, 0.7 * wf.getMaxY()), new Vector2D(-4000, 0.3 * wf.getMaxY())));
        }
        this.visualizerBoxes = waiterBoxList;

        List<IntegratedRobot> tmpRobots = new ArrayList<>(roleRobots);

        Map<Integer, Vector2D> targetPositions = new HashMap<Integer, Vector2D>();

        List<Vector2D> centerPositions = new ArrayList<>();
        for (Pair<Vector2D, Vector2D> box : waiterBoxList) {
            centerPositions.add(box.getFirst().add(box.getSecond()).scalarMultiply(0.5));
        }
        Vector2D nearBallPos = MathHelper.nearestPositionToPosition(centerPositions, ballPos).orElse(null);
        for (Pair<Vector2D, Vector2D> box : waiterBoxList) {
            if (MathHelper.distance2D(box.getFirst().add(box.getSecond()).scalarMultiply(0.5), nearBallPos) < 300) {
                continue;
            }
            if (tmpRobots.isEmpty()) {
                break;
            }
            IntegratedRobot nearest = MathHelper.nearestRobotToPosition(tmpRobots, box.getFirst().add(box.getSecond()).scalarMultiply(0.5)).get();
            List<IntegratedRobot> robotsInBox = oppositeRobots.values().stream().filter(InterfaceHelper.getPredicate(new IFuncParam1<Boolean, IntegratedRobot>() {
                @Override
                public Boolean function(IntegratedRobot robot) {
                    return MathHelper.isCollidedWithBox(new Vector2D(Math.min(box.getFirst().getX(), box.getSecond().getX()),
                            Math.min(box.getFirst().getY(), box.getSecond().getY())), new Vector2D(Math.max(box.getFirst().getX(), box.getSecond().getX()),
                            Math.max(box.getFirst().getY(), box.getSecond().getY())), robot.getRobot().position(), 0);
                }
            })).toList();
            List<Vector2D> positions = new ArrayList<>();
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    positions.add(new Vector2D(Math.min(box.getFirst().getX(), box.getSecond().getX()) + ROBOT_RAD + x / 2.0 * (Math.abs(box.getSecond().getX() - box.getFirst().getX()) - ROBOT_RAD * 2),
                            Math.min(box.getFirst().getY(), box.getSecond().getY()) + ROBOT_RAD + y / 2.0 * (Math.abs(box.getSecond().getY() - box.getFirst().getY()) - ROBOT_RAD * 2)));
                }
            }
            Map<Vector2D, Double> scoreMap = new HashMap<>();
            for (Vector2D position : positions) {
                double score = 0;
                for (IntegratedRobot robot : robotsInBox) {
                    score += MathHelper.distance2D(position, robot.getRobot().position());
                }
                scoreMap.put(position, score);
            }
            Vector2D targetPos = MathHelper.sortPositionsInGreaterScore(scoreMap).getFirst();
            if (robotsInBox.isEmpty()) {
                targetPos = box.getFirst().add(box.getSecond()).scalarMultiply(0.5);
            }
            for (IntegratedRobot robot : robotsInBox) {
                Vector2D oppPos = robot.getRobot().position();
                Vector2D ourPos = nearest.getRobot().position();
                if (MathHelper.distancePositionToRobot(oppPos, nearest.getRobot()) < 1000 &&
                        MathHelper.inferiorAngle(ballPos.subtract(ourPos), oppPos.subtract(ourPos)) > 0.5 * Math.PI) {
                    // 敵がすぐ後ろにいるとき
                    // 敵をマーク
                    targetPos = MathHelper.normalized(ballPos.subtract(oppPos)).scalarMultiply(ROBOT_RAD * 3).add(oppPos);
                    break;
                }
            }
            // フィールド内に収める
            final double margin = 2 * ROBOT_RAD;
            targetPos = new Vector2D(Math.clamp(targetPos.getX(), wf.getMinX() + margin, wf.getMaxX() - margin), Math.clamp(targetPos.getY(), wf.getMinY() + margin, wf.getMaxY() - margin));

            targetPositions.put(nearest.getId(), targetPos);
            tmpRobots.remove(nearest);
        }

        // SimpleSearchで生成された位置をもとに決める
        tmpRobots = new ArrayList<>(roleRobots);
        Map<Integer, Vector2D> passTargetMap = new HashMap<>();
        for (Vector2D target : UpdaterPassTarget.getInstance().getPassTargets(this.color).get()) {
            if (tmpRobots.isEmpty()) {
                break;
            }
            IntegratedRobot nearest = MathHelper.nearestRobotToPosition(tmpRobots, target).get();
            passTargetMap.put(nearest.getId(), target);
            tmpRobots.remove(nearest);
        }

        for (int id : roleIds) {
            if (passTargetMap.containsKey(id)) {
                this.moveMap.get(id).setPos(passTargetMap.get(id));
            } else if (targetPositions.containsKey(id)) {
                this.moveMap.get(id).setPos(targetPositions.get(id));
            } else {
                this.moveMap.get(id).setPos(Vector2D.ZERO);
            }
            this.moveMap.get(id).setAngle(MathHelper.direction(ballPos, friendlyRobots.get(id).getRobot().position()));
            List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, id, CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot());
            if (avoidBall) {
                obstacles.add(new ObstacleCircle(ballPos, 500, 100)); // ボールを避ける
            }
            if (this.widePenaltyArea) {
                // ストップゲーム、セットプレーのとき
                obstacles.add(new ObstacleBox(new Vector2D(wf.getMaxX(), wf.getPenaltyMinY() - 300), new Vector2D(wf.getPenaltyFrontX() - 300, wf.getPenaltyMaxY() + 300), 100.0));
            }
            if (this.ballPlacePos.isPresent()) {
                obstacles.add(new ObstacleSegment(ballPos, this.ballPlacePos.get(), 650));
            }
            if (this.moveMap.get(id).isFinished()) {
                list.add(this.haltMap.get(id));
            } else {
                list.add(new WithPlanner<>(this.moveMap.get(id), id, this.color, obstacles));
            }
        }

        return list;
    }

    @Override
    public String getName() {
        return "role_attack_waiter";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setAvoidBall(boolean avoidBall) {
        this.avoidBall = avoidBall;
    }

    public void setWidePenaltyArea(boolean isWide) {
        this.widePenaltyArea = isWide;
    }

    public void setBallPlacePos(Optional<Vector2D> ballPlacePos) {
        this.ballPlacePos = ballPlacePos;
    }

    /**
     * 待機位置の基準を設定
     * @param nextBallPos
     */
    public void setNextBallPos(Vector2D nextBallPos) {
        this.nextBallPos = Optional.of(nextBallPos);
    }
}