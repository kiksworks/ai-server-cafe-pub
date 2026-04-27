package ai_server_cafe.game.strategy;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.ObstacleTriangle;
import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterRefBox;
import ai_server_cafe.updater.UpdaterStrategy;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExpectedGoals {

    public static final double PI = FastMath.PI;
    final XGValue typeA = new XGValue(0.492, -3.933, 1.164, 0, 0);//2023&2024のデータで学習
    final XGValue typeB = new XGValue(0.567, -3.218, 1.852, -3.282, 0);//2023&2024のデータで学習
    final XGValue typeC = new XGValue(-0.861, -2.49, 0.911, -1.104, 1.549);//2023&2024のデータで学習
    protected Optional<GcRefereeMessage.Referee.TeamInfo> teamInfoBlue;
    protected Optional<GcRefereeMessage.Referee.TeamInfo> teamInfoYellow;
    protected double robotRadius = 90;

    public ExpectedGoals() {

    }

    public double movingPolarCoordinates(@Nonnull Vector2D robotPos, @Nonnull Vector2D originPos, int attackDire) {//原点を移動した極座標の角度を返す関数
        double originX = originPos.getX();
        double robotX = robotPos.getY() * attackDire;
        double robotY = originX * attackDire - robotPos.getX() * attackDire;

        double robotAngle = FastMath.atan2(robotY, robotX);
        robotAngle = 180 * (robotAngle / PI);
        if (robotAngle > 90) {//０〜９０に変換
            robotAngle = 180 - robotAngle;
        }
        return robotAngle;
    }

    public double getNormalization(double value, double max) {
        double norma = value / max;
        if (norma > 1.0) {
            norma = 1.000;
        }
        return FastMath.round(norma * 1000.0) / 1000.0;
    }

    public double derivedXG(@Nonnull XGValue type, double r, double t, double size, double range) {
        double score = type.b() + type.r() * r + type.theta() * t + type.size() * size + type.range() * range;
        return FastMath.round((1 / (1 + FastMath.exp(-score))) * 1000.0) / 1000.0;//シグモイド関数に変換する
    }

    public double derivedXG(@Nonnull XGValue type, double r, double t) {
        return derivedXG(type, r, t, 0.0, 0.0);
    }

    public double derivedXG(@Nonnull XGValue type, double r, double t, double size) {
        return derivedXG(type, r, t, size, 0.0);
    }

    public Vector2D convertVectors(@Nonnull Vector2D oldPos, @Nonnull Vector2D origin) {
        Vector2D convertV;
        convertV = new Vector2D(oldPos.getY(), origin.getX() - oldPos.getX());
        return convertV;
    }

    public Vector2D visibleSegmentLength(@Nonnull Vector2D kicker, @Nonnull IntegratedRobot obstacles, @Nonnull Vector2D goalCenter, double goalWidth, double radius) {
        //可視区間の長さを計算
        Vector2D invisibleTotalLength;
        Vector2D goalR = new Vector2D(goalCenter.getX(), goalCenter.getY() + goalWidth);
        goalR = convertVectors(goalR, goalCenter);
        Vector2D goalL = new Vector2D(goalCenter.getX(), goalCenter.getY() - goalWidth);
        goalL = convertVectors(goalL, goalCenter);
        Vector2D target = convertVectors(kicker, goalCenter);
        Vector2D oppoRobot = new Vector2D(obstacles.getRobot().getX(), obstacles.getRobot().getY());
        Vector2D opponentRobots = convertVectors(oppoRobot, goalCenter);
        //LOGGER.info(target);
        //直線と障害物の影の交点を求める
        Vector2D range;
        double dx = target.getX() - opponentRobots.getX();
        double dy = target.getY() - opponentRobots.getY();
        double dist = FastMath.sqrt(dx * dx + dy * dy);
        if (dist <= radius * 2) {
            range = new Vector2D(-900, 900);//障害物が近すぎる場合はすべて見えないと判定
            return range;
        }
        dx /= dist;
        dy /= dist;

        //接線の接点の座標
        Vector2D tangentA = new Vector2D(opponentRobots.getX() + radius * dy, opponentRobots.getY() - radius * dx);
        Vector2D tangentB = new Vector2D(opponentRobots.getX() - radius * dy, opponentRobots.getY() + radius * dx);

        //　ゴールライン上の線分の２点を計算
        Vector2D f = MathHelper.intersection(goalR, goalL, target, tangentA);
        Vector2D g = MathHelper.intersection(goalR, goalL, target, tangentB);

        //もし２点が存在するなら範囲外の点をゴール幅にクリップする
        double visibleMax;
        double visibleMin;
        if ((f.getX() >= 900 && g.getX() >= 900) || (f.getX() <= -900 && g.getX() <= -900)) {
            range = new Vector2D(-900, -900);//ゴールの外の場合は範囲が０
            return range;
        } else {
            if (f.getX() >= g.getX()) {
                if (f.getX() > 900) {
                    visibleMax = 900;
                } else {
                    visibleMax = f.getX();
                }
                if (g.getX() <= -900) {
                    visibleMin = -900;
                } else {
                    visibleMin = g.getX();
                }
            } else {
                if (g.getX() > 900) {
                    visibleMax = 900;
                } else {
                    visibleMax = g.getX();
                }
                if (f.getX() <= -900) {
                    visibleMin = -900;
                } else {
                    visibleMin = f.getX();
                }
            }
            invisibleTotalLength = new Vector2D(visibleMin, visibleMax);
        }
        return invisibleTotalLength;
    }

    public double mergeRanges(@Nonnull List<Vector2D> ranges, double goalWidth) {
        //範囲をマージする関数
        ranges.sort(Comparator.comparingDouble(Vector2D::getX));
        double maxRange = goalWidth * 2;
        double unVisibleRange = 0;
        // マージ処理
        List<Vector2D> merged = new ArrayList<>();
        merged.add(new Vector2D(ranges.getFirst().getX(), ranges.getFirst().getY()));

        for (int i = 1; i < ranges.size(); i++) {
            Vector2D origin = ranges.get(i);
            Vector2D last = merged.getLast(); // 現在の最後の要素

            if (origin.getX() <= last.getY()) {
                // 重なっている場合、統合して更新
                merged.removeLast(); // 既存の要素を削除
                merged.add(new Vector2D(FastMath.min(last.getX(), origin.getX()), FastMath.max(last.getY(), origin.getY())));
            } else {
                // 重なっていなければ新しく追加
                merged.add(new Vector2D(origin.getX(), origin.getY()));
            }
        }
        for (Vector2D vec : merged) {
            unVisibleRange = unVisibleRange + (vec.getY() - vec.getX());
        }

        return maxRange - unVisibleRange;
    }

    public double calculateXG(Vector2D kicker, Map<Integer, IntegratedRobot> obstacles, int AttackDire, Field wf, XGType type) {
        Vector2D targetGoalCenter;
        if (AttackDire < 0.0) {
            targetGoalCenter = new Vector2D(wf.getMinX(), 0);
        } else {
            targetGoalCenter = new Vector2D(wf.getMaxX(), 0);
        }
        //ゴールの中心を基準に極座標変換
        double robotDist = MathHelper.distance2D(targetGoalCenter, kicker);
        robotDist = getNormalization(robotDist, 6000);
        double robotAngle = movingPolarCoordinates(kicker, targetGoalCenter, AttackDire);
        robotAngle = getNormalization(robotAngle, 90);
        int blockerSize = 0;
        List<Vector2D> unavailableViewpoint = new ArrayList<>();
        Vector2D goalR = new Vector2D(targetGoalCenter.getX(),targetGoalCenter.getY()+wf.getGoalLength()/2);
        Vector2D goalL = new Vector2D(targetGoalCenter.getX(),targetGoalCenter.getY()-wf.getGoalLength()/2);
        for (Map.Entry<Integer, IntegratedRobot> oppo : obstacles.entrySet()) {
            //Integer keyE = oppo.getKey();   // キー (Integer)
            IntegratedRobot obstacleRobot = oppo.getValue();// 値 (IntegratedRobot)
            Vector2D opponent = new Vector2D(obstacleRobot.getRobot().getX(), obstacleRobot.getRobot().getY());
            if (new ObstacleTriangle(kicker, goalR, goalL, CommonObstacles.getMarginRobot()).isCollided(opponent)) {
                //blockerRobots.put(keyE,robotE);
                blockerSize++;
                Vector2D point = visibleSegmentLength(kicker, obstacleRobot, targetGoalCenter, wf.getGoalMaxY(), robotRadius);
                unavailableViewpoint.add(point);
            }
        }
        double blockerSizeNorm = getNormalization(blockerSize, 7);
        double visibleBlueRange = 1800;
        if (!unavailableViewpoint.isEmpty()) {
            visibleBlueRange = mergeRanges(unavailableViewpoint, wf.getGoalMaxY());
        }
        visibleBlueRange = getNormalization(visibleBlueRange, 1800);
        if (type == XGType.TYPEC) {
            return derivedXG(typeC, robotDist, robotAngle, blockerSizeNorm, visibleBlueRange);
        } else if (type == XGType.TYPEB) {
            return derivedXG(typeB, robotDist, robotAngle, blockerSizeNorm);
        } else {
            return derivedXG(typeA, robotDist, robotAngle);
        }
    }

    public void update() {
        final World world = UpdaterWorld.getInstance().getWorld(false);
        final Field wf = world.getField();

        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        //final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;

        //final Vector2D ballPos = world.getBall().position();
        //final Vector2D ballVel = world.getBall().velocity();
        Map<Integer, IntegratedRobot> blueRobots = world.getFriendlyRobotMap(TeamColor.BLUE);
        Map<Integer, IntegratedRobot> yellowRobots = world.getFriendlyRobotMap(TeamColor.YELLOW);

        int blueAttackDire;
        int yellowAttackDire;

        teamInfoBlue = UpdaterRefBox.getInstance().getTeamInfo(TeamColor.BLUE).getInfo();
        teamInfoYellow = UpdaterRefBox.getInstance().getTeamInfo(TeamColor.YELLOW).getInfo();

        UpdaterStrategy xg = UpdaterStrategy.getInstance();
        xg.resetXG();

        //攻撃方向を判定
        // blueTeamのGKを特定し攻撃方向を推定
        boolean blueGoalNegative = ConfigManager.getInstance().isInvert(TeamColor.BLUE);
        if (teamInfoBlue.isPresent()) {
            int blueKeeperId = teamInfoBlue.get().getGoalkeeper();

            IntegratedRobot blueGkRobot = blueRobots.get(blueKeeperId);
            if (blueGkRobot != null) {
                double x = blueGkRobot.getRobot().getX();
                if (x < 0.0) {
                    // GKの位置が負なら攻撃フィールドは正
                    blueAttackDire = 1;
                } else {
                    blueAttackDire = -1;
                }
            } else {
                //LOGGER.info("blueのチーム情報が取得できません(blueのGKが見つかりません)");
                blueAttackDire = blueGoalNegative ? 1 : -1;
            }
        } else {
            //LOGGER.info("blueのチーム情報が取得できません");
            blueAttackDire = blueGoalNegative ? 1 : -1;
        }

        //yellowTeamのGKを特定し攻撃方向を推定
        boolean yellowGoalNegative = ConfigManager.getInstance().isInvert(TeamColor.YELLOW);
        if (teamInfoYellow.isPresent()) {
            int blueKeeperId = teamInfoYellow.get().getGoalkeeper();

            IntegratedRobot YellowGkRobot = yellowRobots.get(blueKeeperId);
            if (YellowGkRobot != null) {
                double x = YellowGkRobot.getRobot().getX();
                if (x < 0.0) {
                    // GKの位置が負なら攻撃フィールドは正
                    yellowAttackDire = 1;
                } else {
                    yellowAttackDire = -1;
                }
            } else {
                //LOGGER.info("yellowのチーム情報が取得できません(yellowのGKが見つかりません)");
                yellowAttackDire = yellowGoalNegative ? 1 : -1;
            }
        } else {
            //LOGGER.info("yellowのチーム情報が取得できません");
            yellowAttackDire = yellowGoalNegative ? 1 : -1;
        }

        if (blueAttackDire == yellowAttackDire) {
            blueAttackDire = blueGoalNegative ? 1 : -1;
            yellowAttackDire = yellowGoalNegative ? 1 : -1;
            //LOGGER.info("攻撃方向が推定できません");
        }
        xg.setAttackDire(blueAttackDire, TeamColor.BLUE);
        xg.setAttackDire(yellowAttackDire, TeamColor.YELLOW);

        // ボールに一番近いロボット
        /*
        Optional<IntegratedRobot> blueNearest = MathHelper.nearestRobotToPosition(blueRobots, ballPos);
        Optional<IntegratedRobot> yellowNearest = MathHelper.nearestRobotToPosition(yellowRobots, ballPos);
        boolean blueHaveBall = blueNearest.isPresent()
                && MathHelper.distancePositionToRobot(ballPos, blueNearest.get().getRobot()) < ROBOT_RAD * 2;
        boolean yellowHaveBall = yellowNearest.isPresent()
                && MathHelper.distancePositionToRobot(ballPos, yellowNearest.get().getRobot()) < ROBOT_RAD * 2;
        */

        for (Map.Entry<Integer, IntegratedRobot> entry : blueRobots.entrySet()) {
            Integer key = entry.getKey();   // キー (Integer)
            IntegratedRobot robot = entry.getValue();// 値 (IntegratedRobot)
            Vector2D bluePos = robot.getRobot().position();
            xg.setXG(calculateXG(bluePos, yellowRobots,  UpdaterStrategy.getInstance().getAttackDire(TeamColor.BLUE), wf, XGType.TYPEA), TeamColor.BLUE, key, XGType.TYPEA);
            xg.setXG(calculateXG(bluePos, yellowRobots,  UpdaterStrategy.getInstance().getAttackDire(TeamColor.BLUE), wf, XGType.TYPEB), TeamColor.BLUE, key, XGType.TYPEB);
            xg.setXG(calculateXG(bluePos, yellowRobots,  UpdaterStrategy.getInstance().getAttackDire(TeamColor.BLUE), wf, XGType.TYPEC), TeamColor.BLUE, key, XGType.TYPEC);
        }

        for (Map.Entry<Integer, IntegratedRobot> entry : yellowRobots.entrySet()) {
            Integer key = entry.getKey();   // キー (Integer)
            IntegratedRobot robot = entry.getValue();// 値 (IntegratedRobot)
            Vector2D yellowPos = robot.getRobot().position();
            xg.setXG(calculateXG(yellowPos, blueRobots,  UpdaterStrategy.getInstance().getAttackDire(TeamColor.YELLOW), wf, XGType.TYPEA), TeamColor.YELLOW, key, XGType.TYPEA);
            xg.setXG(calculateXG(yellowPos, blueRobots,  UpdaterStrategy.getInstance().getAttackDire(TeamColor.YELLOW), wf, XGType.TYPEB), TeamColor.YELLOW, key, XGType.TYPEB);
            xg.setXG(calculateXG(yellowPos, blueRobots,  UpdaterStrategy.getInstance().getAttackDire(TeamColor.YELLOW), wf, XGType.TYPEC), TeamColor.YELLOW, key, XGType.TYPEC);
        }

        // 目標位置の候補
        List<Vector2D> positions = new ArrayList<>();
        // 格子点
        for (int x = 4; x < 10; x++) {
            for (int y = 1; y < 10; y++) {
                positions.add(new Vector2D((wf.getMaxX() - wf.getMinX()) * x / 10 + wf.getMinX(), (wf.getMaxY() - wf.getMinY()) * y / 10 + wf.getMinY()));
            }
        }
        // 相手ゴール正面
        for (int i = 0; i <= 10; i++) {
            positions.add(new Vector2D(wf.getPenaltyFrontX() - ROBOT_RAD * 2, wf.getPenaltyMaxY() * (i - 5) / 5));
        }
        // 相手ゴール横
        for (int i = 0; i < 5; i++) {
            positions.add(new Vector2D(wf.getPenaltyFrontX() + wf.getPenaltyLength() * i / 5, wf.getPenaltyMaxY() + ROBOT_RAD * 2));
            positions.add(new Vector2D(wf.getPenaltyFrontX() + wf.getPenaltyLength() * i / 5, wf.getPenaltyMinY() - ROBOT_RAD * 2));
        }
        //ポイントごとのゴール期待値
        Map<Vector2D, Double> scoreMapBlueA = new HashMap<>();
        Map<Vector2D, Double> scoreMapYellowA = new HashMap<>();
        Map<Vector2D, Double> scoreMapBlueB = new HashMap<>();
        Map<Vector2D, Double> scoreMapYellowB = new HashMap<>();
        Map<Vector2D, Double> scoreMapBlueC = new HashMap<>();
        Map<Vector2D, Double> scoreMapYellowC = new HashMap<>();

        for (Vector2D pos : positions) {
            if ((pos.getX() > wf.getPenaltyFrontX() - ROBOT_RAD && FastMath.abs(pos.getY()) < wf.getPenaltyMaxY() + ROBOT_RAD)) {
                // ペナルティーエリア内は除外
                // すぐ近くのパス候補は除外
                // 遠すぎるパス候補は除外
                continue;
            }
            scoreMapBlueA.put(pos, calculateXG(pos, yellowRobots,1, wf, XGType.TYPEA));
            scoreMapBlueB.put(pos, calculateXG(pos, yellowRobots,1, wf, XGType.TYPEB));
            scoreMapBlueC.put(pos, calculateXG(pos, yellowRobots,1, wf, XGType.TYPEC));
        }
        for (Vector2D pos : positions) {
            if ((pos.getX() > wf.getPenaltyFrontX() - ROBOT_RAD && FastMath.abs(pos.getY()) < wf.getPenaltyMaxY() + ROBOT_RAD)) {
                // ペナルティーエリア内は除外
                continue;
            }//UpdaterStrategy.getInstance().getAttackDire(TeamColor.YELLOW)
            scoreMapYellowA.put(pos, calculateXG(pos, blueRobots,  1, wf, XGType.TYPEA));
            scoreMapYellowB.put(pos, calculateXG(pos, blueRobots,  1, wf, XGType.TYPEB));
            scoreMapYellowC.put(pos, calculateXG(pos, blueRobots,  1, wf, XGType.TYPEC));
        }

        UpdaterStrategy.getInstance().setXGMap(scoreMapBlueA, TeamColor.BLUE, XGType.TYPEA);
        UpdaterStrategy.getInstance().setXGMap(scoreMapYellowA, TeamColor.YELLOW, XGType.TYPEA);
        UpdaterStrategy.getInstance().setXGMap(scoreMapBlueB, TeamColor.BLUE, XGType.TYPEB);
        UpdaterStrategy.getInstance().setXGMap(scoreMapYellowB, TeamColor.YELLOW, XGType.TYPEB);
        UpdaterStrategy.getInstance().setXGMap(scoreMapBlueC, TeamColor.BLUE, XGType.TYPEC);
        UpdaterStrategy.getInstance().setXGMap(scoreMapYellowC, TeamColor.YELLOW, XGType.TYPEC);
    }

    record XGValue(double b, double r, double theta, double size, double range) {
    }
}
