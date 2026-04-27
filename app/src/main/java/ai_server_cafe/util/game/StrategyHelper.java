package ai_server_cafe.util.game;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StrategyHelper {

    /**
     * 有効なシュートコースを見つける
     * @param kickerPosition ボールをけるロボットの位置
     * @param num ゴールをいくつに分割するか
     * @return シュート目標のリスト
     */
    public static List<Vector2D> findShootTarget(World world, TeamColor color, Vector2D kickerPosition, double margin, int num) {
        final Field wf = world.getField();
        final Map<Integer, IntegratedRobot> oppositeRobots = world.getOppositeRobotMap(color);
        List<Vector2D> list = new ArrayList<>();
        num = FastMath.max(3, num);
        for (int i = 1; i < num; i++) {
            Vector2D target = new Vector2D(wf.getMaxX(), wf.getGoalMinY() + wf.getGoalWidth() * i / num);
            if (!isLineInterrupted(kickerPosition, margin, target, oppositeRobots.values().stream().toList())) {
                list.add(target);
            }
        }
        return list;
    }

    /**
     * 有効なシュートコースを見つける
     * @param kickerPosition ボールをけるロボットの位置
     * @return シュート目標のリスト
     */
    public static List<Vector2D> findShootTarget(World world, TeamColor color, Vector2D kickerPosition) {
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        return findShootTarget(world, color, kickerPosition, ROBOT_RAD + BALL_RAD * 3, 10);
    }

    /**
     * シュート目標をソート
     * @param world
     * @param color
     * @param targetPositions
     * @param kickerPosition
     * @param kickerDirection
     * @return
     */
    public static List<Vector2D> sortShootTarget(World world, TeamColor color, List<Vector2D> targetPositions, Vector2D kickerPosition, double kickerDirection) {
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final Field wf = world.getField();
        final Map<Integer, IntegratedRobot> oppositeRobots = world.getOppositeRobotMap(color);
        Map<Vector2D, Double> scoreMap = new HashMap<>();
        if (oppositeRobots.isEmpty()) {
            for (Vector2D target : targetPositions) {
                scoreMap.put(target, -MathHelper.distance2D(target, wf.getFrontGoalCenter()));
            }
        } else {
            IntegratedRobot keeper = MathHelper.nearestRobotToPosition(oppositeRobots, wf.getFrontGoalCenter()).get();
            for (Vector2D target : targetPositions) {
                scoreMap.put(target, FastMath.exp(-MathHelper.inferiorAngle(MathHelper.direction(wf.getFrontGoalCenter(), kickerPosition), MathHelper.direction(target, kickerPosition)))
                        -1.5 * FastMath.exp(-MathHelper.inferiorAngle(MathHelper.direction(target, kickerPosition), MathHelper.direction(keeper.getRobot().position(), kickerPosition))));
            }
        }

        return MathHelper.sortPositionsInGreaterScore(scoreMap);
    }

    /**
     * 有効なパス目標を見つける
     * @param waiterPositions 候補（ロボットの待機位置）
     * @param kickerPosition ボールをけるロボットの位置
     * @return パス目標のリスト
     */
    public static List<Vector2D> findPassTarget(World world, TeamColor color, List<Vector2D> waiterPositions, Vector2D kickerPosition) {
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        // チップの最大飛距離
        final double chipDistance = 1500;
        final Field wf = world.getField();
        final Map<Integer, IntegratedRobot> oppositeRobots = world.getOppositeRobotMap(color);

        List<Vector2D> positions = new ArrayList<>();
        for (Vector2D position : waiterPositions) {
            // ディフェンスは除外
            if (position.getX() > wf.getMinX() / 3 && (position.getX() > kickerPosition.getX() - 300.0 || position.getX() > 0.5 * wf.getMaxX())) {
                positions.add(position);
            }
        }

        List<Vector2D> list = new ArrayList<>();
        // すぐにシュートできるパス目標
        for (Vector2D target : positions) {
            if (!findShootTarget(world, color, target).isEmpty() && MathHelper.distance2D(target, wf.getFrontGoalCenter()) < wf.getMaxY() &&
                    !isLineInterrupted(kickerPosition, target, oppositeRobots.values().stream().toList())) {
                list.add(target);
            }
        }
        if (!list.isEmpty()) return list;

        // すぐにシュートできるパス目標（チップ）
        for (Vector2D target : positions) {
            if (!findShootTarget(world, color, target).isEmpty() && MathHelper.distance2D(target, wf.getFrontGoalCenter()) < wf.getMaxY() &&
                    (MathHelper.distance2D(target, kickerPosition) < chipDistance
                            || (!isLineInterrupted(kickerPosition, kickerPosition.add(MathHelper.normalized(target.subtract(kickerPosition)).scalarMultiply(ROBOT_RAD * 3)), oppositeRobots.values().stream().toList()) &&
                            !isLineInterrupted(kickerPosition.add(MathHelper.normalized(target.subtract(kickerPosition)).scalarMultiply(chipDistance)), target, oppositeRobots.values().stream().toList())))) {
                list.add(target);
            }
        }
        if (!list.isEmpty()) return list;

        // すぐにシュートできないパス目標
        for (Vector2D target : positions) {
            if (!isLineInterrupted(kickerPosition, target, oppositeRobots.values().stream().toList())) {
                list.add(target);
            }
        }
        if (!list.isEmpty()) return list;

        // すぐにシュートできないパス目標（チップ）
        for (Vector2D target : positions) {
            if (MathHelper.distance2D(target, kickerPosition) < chipDistance
                            || (!isLineInterrupted(kickerPosition, kickerPosition.add(MathHelper.normalized(target.subtract(kickerPosition)).scalarMultiply(ROBOT_RAD * 3)), oppositeRobots.values().stream().toList()) &&
                            !isLineInterrupted(kickerPosition.add(MathHelper.normalized(target.subtract(kickerPosition)).scalarMultiply(chipDistance)), target, oppositeRobots.values().stream().toList()))) {
                list.add(target);
            }
        }
        return list;
    }

    /**
     * パス目標をソート
     * @param world
     * @param color
     * @param targetPositions
     * @param kickerPosition
     * @param kickerDirection
     * @return
     */
    public static List<Vector2D> sortPassTarget(World world, TeamColor color, List<Vector2D> targetPositions, Vector2D kickerPosition, double kickerDirection) {
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final Field wf = world.getField();
        final Map<Integer, IntegratedRobot> oppositeRobots = world.getOppositeRobotMap(color);
        Map<Vector2D, Double> scoreMap = new HashMap<>();
        for (Vector2D target : targetPositions) {
            scoreMap.put(target, FastMath.exp(-MathHelper.inferiorAngle(kickerDirection, MathHelper.direction(target, kickerPosition))));
        }
        return MathHelper.sortPositionsInGreaterScore(scoreMap);
    }

    /**
     * 有効なドリブル目標を見つける
     * @param kickerPosition ボールをけるロボットの位置
     * @return ドリブル目標のリスト
     */
    public static List<Vector2D> findDribbleTarget(World world, TeamColor color, Vector2D kickerPosition) {
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final Field wf = world.getField();
        final Map<Integer, IntegratedRobot> oppositeRobots = world.getOppositeRobotMap(color);

        List<Vector2D> positions = new ArrayList<>();
        List<Vector2D> shootPositions = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            // シュートしやすい位置
            shootPositions.add(new Vector2D(wf.getPenaltyFrontX() - 500, i / 3.0 * wf.getPenaltyMaxY()));
        }
        for (Vector2D position : shootPositions) {
            Vector2D target = MathHelper.normalized(position.subtract(kickerPosition))
                    .scalarMultiply(FastMath.min(1500, MathHelper.distance2D(position, kickerPosition))).add(kickerPosition);
            if (!isDirectionInDanger(kickerPosition, target, oppositeRobots.values().stream().toList())) {
                positions.add(target);
            }
        }
        return positions;
    }

    /**
     * ドリブル目標をソート
     * @param world
     * @param color
     * @param targetPositions
     * @param kickerPosition
     * @param kickerDirection
     * @return
     */
    public static List<Vector2D> sortDribbleTarget(World world, TeamColor color, List<Vector2D> targetPositions, Vector2D kickerPosition, double kickerDirection) {
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final Field wf = world.getField();
        final Map<Integer, IntegratedRobot> oppositeRobots = world.getOppositeRobotMap(color);
        Map<Vector2D, Double> scoreMap = new HashMap<>();
        for (Vector2D target : targetPositions) {
            scoreMap.put(target, FastMath.exp(-MathHelper.inferiorAngle(kickerDirection, MathHelper.direction(target, kickerPosition))));
        }
        return MathHelper.sortPositionsInGreaterScore(scoreMap);
    }
    /**
     *
     * @param world     World
     * @param color     TeamColor
     * @param kicker    kickerRobot
     * @param oppRobot  oppositeRobot
     * @return 自分の前方向に対して有効な回避目標を見つける
     */
    public static List<Vector2D> findAvoidTarget(World world, TeamColor color,
                                                 IntegratedRobot kicker, IntegratedRobot oppRobot) {
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final Field wf = world.getField();
        final double HALF_LENGTH = wf.getMaxY()/2;
        final Vector2D fMin = new Vector2D(wf.getMinX(), wf.getMinY());
        final Vector2D fMax = new Vector2D(wf.getMaxX(), wf.getMaxY());
        final Vector2D kickerPos = kicker.getRobot().position();
        final Vector2D oppPos = oppRobot.getRobot().position();

        List<Vector2D> avoidPositions = new ArrayList<>();
        List<Vector2D> positions = new ArrayList<>();
        for (int i = -1; i <= 1 && i != 0; i++) {
            // 回避しやすい位置
            avoidPositions.add(new Vector2D(kickerPos.getX() + 1000,
                    oppPos.getY() + i/3.0*wf.getPenaltyMaxY()));
        }
        for(Vector2D target : avoidPositions) {
            if(FastMath.abs(target.getY()) < HALF_LENGTH
                    && !isLineInterrupted(kicker.getRobot().position(), ROBOT_RAD * 4,  target, Arrays.asList(oppRobot))
                    && MathHelper.isCollidedWithBox(fMin, fMax, target, 2*ROBOT_RAD)) {
                positions.add(target);
            }
        }
        return positions;
    }

    /**
     *
     * @param world            world
     * @param color            kickerRobotの色
     * @param targetPositions  回避目標リスト
     * @param kicker
     * @param oppRobot
     * @return 有効な回避目標をソート
     */
    public static List<Vector2D> sortAvoidTarget(World world, TeamColor color, List<Vector2D> targetPositions,
                                                 IntegratedRobot kicker, IntegratedRobot oppRobot) {
        // ボール半径
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        // ロボット中心からキッカー表面までの距離
        final double TO_FACE_RAD = ConfigManager.getInstance().getConfig().toFaceRadius;
        final Vector2D oppFacePos = oppRobot.getRobot().position().add(MathHelper.getFromPolar(BALL_RAD+TO_FACE_RAD, oppRobot.getRobot().getTheta()));
        Map<Vector2D, Double> scoreMap = new HashMap<>();
        for(Vector2D target : targetPositions) {
            scoreMap.put(target, -FastMath.exp(-MathHelper.distance2D(target, oppFacePos)));
        }
        return MathHelper.sortPositionsInGreaterScore(scoreMap);
    }

    /**
     * クリア目標を見つける
     * @param kickerPosition ボールをけるロボットの位置
     * @return クリア目標のリスト
     */
    public List<Vector2D> findClearTarget(World world, TeamColor color, Vector2D kickerPosition) {
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final Field wf = world.getField();
        final Map<Integer, IntegratedRobot> oppositeRobots = world.getOppositeRobotMap(color);
        List<Vector2D> list = new ArrayList<>();
        return list;
    }

    /**
     * パスコースがロボットに遮られているか
     * @param kickerPosition
     * @param margin
     * @param target
     * @param robots
     * @return 遮っているロボットがi台でもいればtrue
     */
    public static boolean isLineInterrupted(Vector2D kickerPosition, double margin, Vector2D target, List<IntegratedRobot> robots) {
        return robots.stream().anyMatch(
                InterfaceHelper.getPredicate(new IFuncParam1<Boolean, IntegratedRobot>() {
                    @Override
                    public Boolean function(IntegratedRobot robot) {
                        return MathHelper.isCollidedWithSegment(kickerPosition, target, robot.getRobot().position(), margin);
                    }
                }));
    }

    /**
     * パスコースがロボットに遮られているか
     * @param kickerPosition
     * @param target
     * @param robots
     * @return 遮っているロボットがi台でもいればtrue
     */
    public static boolean isLineInterrupted(Vector2D kickerPosition, Vector2D target, List<IntegratedRobot> robots) {
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        return isLineInterrupted(kickerPosition, BALL_RAD * 3 + ROBOT_RAD, target, robots);
    }

    /**
     * ドリブル目標がロボットに遮られているか
     * @param kickerPosition
     * @param target
     * @param robots
     * @return 遮っているロボットがi台でもいればtrue
     */
    public static boolean isDirectionInDanger(Vector2D kickerPosition, Vector2D target, List<IntegratedRobot> robots) {
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        return robots.stream().anyMatch(
                InterfaceHelper.getPredicate(new IFuncParam1<Boolean, IntegratedRobot>() {
                    @Override
                    public Boolean function(IntegratedRobot robot) {
                        final Vector2D robotPos = robot.getRobot().position();
                        return MathHelper.distance2D(kickerPosition, robotPos) <
                                MathHelper.distance2D(target, kickerPosition)
                                && MathHelper.inferiorAngle(MathHelper.direction(robotPos.subtract(kickerPosition)),
                                MathHelper.direction(target.subtract(kickerPosition))) < 0.2 * FastMath.PI;
                    }
                }));
    }

    /**
     * 有効なドリフト目標を見つける
     * @param dribbleStartPos ドリブル開始地点
     * @return ドリフト目標のリスト
     */
    public static List<Vector2D> findDriftTarget(World world, TeamColor color, Vector2D dribbleStartPos, Vector2D robotPos) {
        final double DRIBBLE_MARGIN = 800;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final Map<Integer, IntegratedRobot> oppositeRobots = world.getOppositeRobotMap(color);
        List<Vector2D> targets = new ArrayList<>();
        for (int i = -5; i <= 5 ; i++) {
            targets.add(new Vector2D(dribbleStartPos.getX(), dribbleStartPos.getY() + DRIBBLE_MARGIN * i / 5));
        }
        for (int i = -5; i <= 5 ; i++) {
            targets.add(new Vector2D(dribbleStartPos.getX() + DRIBBLE_MARGIN * i / 5, dribbleStartPos.getY()));
        }
        List<Vector2D> list = new ArrayList<>();
        for (Vector2D target : targets) {
            Optional<IntegratedRobot> nearest = MathHelper.nearestRobotToPosition(oppositeRobots, target);
            if (!findShootTarget(world, color, target).isEmpty()
                    && MathHelper.distance2D(robotPos, target) < ROBOT_RAD * 5
                    && !isLineInterrupted(robotPos, ROBOT_RAD * 5, target, oppositeRobots.values().stream().toList())
                    && !(nearest.isPresent() && MathHelper.distancePositionToRobot(target, nearest.get().getRobot()) < ROBOT_RAD * 5)) {
                list.add(target);
            }
        }
        return list;
    }

    /**
     * ドリフト目標をソート
     * @param world
     * @param color
     * @param targetPositions
     * @param kickerPosition
     * @return
     */
    public static List<Vector2D> sortDriftTarget(World world, TeamColor color, List<Vector2D> targetPositions, Vector2D kickerPosition, Vector2D kickerVelocity) {
        Map<Vector2D, Double> scoreMap = new HashMap<>();
        for (Vector2D target : targetPositions) {
            scoreMap.put(target, -MathHelper.distance2D(target, kickerPosition) - MathHelper.distance2D(target, kickerPosition.add(kickerVelocity)));
        }
        return MathHelper.sortPositionsInGreaterScore(scoreMap);
    }
}
