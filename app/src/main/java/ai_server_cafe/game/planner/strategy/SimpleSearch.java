package ai_server_cafe.game.planner.strategy;

import ai_server_cafe.config.Config;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterPassTarget;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// アタッカーの待機位置およびパス目標を生成するクラス
public class SimpleSearch {
    private final TeamColor color;

    public SimpleSearch(TeamColor color) {
        this.color = color;
    }

    public void execute() {
        if (UpdaterPassTarget.getInstance().getTargetLocked(this.color)) {
            // ロックされていたら更新しない
            return;
        }

        Config config = ConfigManager.getInstance().getConfig();
        World world = UpdaterWorld.getInstance().getWorld(ConfigManager.getInstance().isInvert(this.color));
        Field field = world.getField();

        final double ROBOT_RAD = config.robotRadius;
        Vector2D ballPos = world.getBall().position();
        Vector2D ballVel = world.getBall().velocity();
        Map<Integer, IntegratedRobot> friendlyRobots = world.getFriendlyRobotMap(color);
        Map<Integer, IntegratedRobot> oppositeRobots = world.getOppositeRobotMap(color);

        // 目標位置の候補
        List<Vector2D> positions = new ArrayList<>();
        // 格子点
        for (int x = 4; x < 10; x++) {
            for (int y = 1; y < 10; y++) {
                positions.add(new Vector2D((field.getMaxX() - field.getMinX()) * x / 10 + field.getMinX(),
                        (field.getMaxY() - field.getMinY()) * y / 10 + field.getMinY()));
            }
        }
        // 相手ゴール正面
        for (int i = 0; i <= 10; i++) {
            positions.add(new Vector2D(field.getPenaltyFrontX() - ROBOT_RAD * 2, field.getPenaltyMaxY() * (i - 5) / 5));
        }
        // 相手ゴール横
        for (int i = 0; i < 5; i++) {
            positions.add(new Vector2D(field.getPenaltyFrontX() + field.getPenaltyLength() * i / 5, field.getPenaltyMaxY() + ROBOT_RAD * 2));
            positions.add(new Vector2D(field.getPenaltyFrontX() + field.getPenaltyLength() * i / 5, field.getPenaltyMinY() - ROBOT_RAD * 2));
        }

        // スコアをつける
        Map<Vector2D, Double> scoreMap = new HashMap<>();
        for (Vector2D pos : positions) {
            double toBallDist = MathHelper.distance2D(ballPos, pos);
            if ((pos.getX() > field.getPenaltyFrontX() - ROBOT_RAD && Math.abs(pos.getY()) < field.getPenaltyMaxY() + ROBOT_RAD)
                    || 1000 > toBallDist || toBallDist > 6000) {
                // ペナルティーエリア内は除外
                // すぐ近くのパス候補は除外
                // 遠すぎるパス候補は除外
                continue;
            }

            // 周りにいる敵の数
            int oppCount = ((int) oppositeRobots.values().stream().filter(InterfaceHelper.getPredicate(
                    new IFuncParam1<Boolean, IntegratedRobot>() {
                        @Override
                        public Boolean function(IntegratedRobot integratedRobot) {
                            return MathHelper.distancePositionToRobot(pos, integratedRobot.getRobot()) < 1000.0;
                        }
                    }
            )).count());

            // すぐ近くにいる敵の数
            int nearbyOppCount =
                    ((int) oppositeRobots.values().stream().filter(InterfaceHelper.getPredicate(
                            new IFuncParam1<Boolean, IntegratedRobot>() {
                                @Override
                                public Boolean function(IntegratedRobot integratedRobot) {
                                    return MathHelper.distancePositionToRobot(pos, integratedRobot.getRobot()) < ROBOT_RAD * 4;
                                }
                            }
                    )).count());

            // ゴールとの間にいる敵の数
            int toGoalCount =
                    ((int) oppositeRobots.values().stream().filter(InterfaceHelper.getPredicate(
                            new IFuncParam1<Boolean, IntegratedRobot>() {
                                @Override
                                public Boolean function(IntegratedRobot integratedRobot) {
                                    Vector2D oppPos = integratedRobot.getRobot().position();
                                    return oppPos.getX() < field.getMaxX()
                                            && MathHelper.direction(field.getFrontGoalRight(), pos) <
                                            MathHelper.direction(oppPos.subtract(pos))
                                            && MathHelper.direction(oppPos.subtract(pos)) <
                                            MathHelper.direction(field.getFrontGoalLeft(), pos);
                                }
                            }
                    )).count());

            // chaserとの間にいる敵の数
            // キーパーは数えない
            int wallCount =
                    ((int) oppositeRobots.values().stream().filter(InterfaceHelper.getPredicate(
                            new IFuncParam1<Boolean, IntegratedRobot>() {
                                @Override
                                public Boolean function(IntegratedRobot integratedRobot) {
                                    Vector2D oppPos = integratedRobot.getRobot().position();
                                    return MathHelper.distance2D(oppPos, field.getFrontGoalCenter()) > 0.5 * field.getGoalWidth()
                                            && MathHelper.inferiorAngle(pos.subtract(ballPos), oppPos.subtract(ballPos)) < 0.1 * Math.PI
                                            && MathHelper.distance2D(oppPos, ballPos) < MathHelper.distance2D(pos, ballPos);
                                }
                            }
                    )).count());

            // kick_posとの距離
            double distanceToKickPos = MathHelper.distance2D(pos, ballPos);
            // 敵ゴールとの距離
            double distanceToFront = MathHelper.distance2D(pos, field.getFrontGoalCenter());
            // 味方ゴールとの距離
            double distanceToBack = MathHelper.distance2D(pos, field.getBackGoalCenter());
            // 敵ゴールとの角度
            double angleToFront =
                    MathHelper.inferiorAngle(pos.subtract(field.getFrontGoalCenter()), new Vector2D(-1, 0));

            // 重み付け
            double score = - 16 * oppCount - 8 * nearbyOppCount
                    - 16 * toGoalCount - 40 * wallCount
                    - 500 * Math.exp(-distanceToBack / 4000)
                    + 10 * Math.exp(-distanceToFront / 100)
                    + 180 * Math.exp(-angleToFront / 1.5)
                    + 80 * Math.exp(-Math.abs(angleToFront - 0.25 * Math.PI) / 0.5)
                    + 30 * Math.exp(-distanceToKickPos / 5000);
            for (Vector2D prePos : UpdaterPassTarget.getInstance().getPassTargets(this.color).get()) {
                // 前回の位置に近いもののスコアを高く
                score += 50 * Math.exp(-MathHelper.distance2D(prePos, pos) / 100);
            }
            scoreMap.put(pos, score);
        }

        if (scoreMap.isEmpty()) return;

        // スコアの大きい順に並べる
        List<Vector2D> sortedPositions = MathHelper.sortPositionsInGreaterScore(scoreMap);

        List<Vector2D> passTargets = new ArrayList<>();

        // 第一候補
        Vector2D passTarget = sortedPositions.getFirst();
        passTargets.add(passTarget);

        // 第一候補に近いものを除外
        List<Vector2D> secondPositions = sortedPositions.stream().filter(InterfaceHelper.getPredicate(
                new IFuncParam1<Boolean, Vector2D>() {
                    @Override
                    public Boolean function(Vector2D pos) {
                        return MathHelper.distance2D(pos, passTarget) > 2000;
                    }
                }
        )).toList();

        // 第２候補
        if (!secondPositions.isEmpty()) {
            passTargets.add(secondPositions.getFirst());
        }

        // updaterへ登録
        UpdaterPassTarget.getInstance().setScoreMap(scoreMap, this.color);
        UpdaterPassTarget.getInstance().setPassTargets(passTargets, this.color);

    }
}
