package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.model.field.obstacle.base.ObstacleSegment;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.IFuncParam2;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoleDefense extends AbstractRole {
    // 壁ロボットの状態の表現
    private enum WallerState {
        MOVE, // 移動中
        BLOCK// ブロック中
    };

    private boolean avoidBall;
    private Optional<Vector2D> ballPlacePos;
    private Vector2D wallTarget;
    private Vector2D prevWallTarget;
    private Vector2D wallCenter;
    private List<AbstractMap.SimpleEntry<Integer, WallerState>> wallers;
    private Optional<Integer> wallSize;

    public RoleDefense(TeamColor color, int[] ids) {
        super(color, ids);
        this.avoidBall = false;
        this.ballPlacePos = Optional.empty();
        this.wallTarget = Vector2D.ZERO;
        this.prevWallTarget = Vector2D.ZERO;
        this.wallCenter = Vector2D.ZERO;
        this.wallers = new ArrayList<>();
        this.wallSize = Optional.empty();
    }

    @Override
    public List<AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty() || roleIds.isEmpty()) {
            return list;
        }
        List<Integer> visibleIds = getVisibleIds(this.world.get(), this.color, this.activeRobots);
        Map<Integer, IntegratedRobot> friendlyRobots = this.world.get().getFriendlyRobotMap(color);
        Map<Integer, IntegratedRobot> oppositeRobots = this.world.get().getOppositeRobotMap(color);

        List<AbstractMap.SimpleEntry<Integer, WallerState>> tempWaller = new ArrayList<>(this.wallers);
        this.wallers.clear();
        for (AbstractMap.SimpleEntry<Integer, WallerState> waller : tempWaller) {
            if (visibleIds.contains(waller.getKey())) {
                this.wallers.add(waller);
            }
        }
        tempWaller = null;

        final Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();

        final Field wf = this.world.get().getField();
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;

        final Vector2D goal = wf.getBackGoalCenter();
        final Vector2D goalLeft = new Vector2D(wf.getMinX(), wf.getGoalMaxY());
        final Vector2D goalRight = new Vector2D(wf.getMinX(), wf.getGoalMinY());
        final double toLeft = MathHelper.direction(goalLeft.subtract(ballPos));
        final double toRight = MathHelper.direction(goalRight.subtract(ballPos));

        final double penaltyCornerTheta =
                MathHelper.inferiorAngle(new Vector2D(wf.getPenaltyBackX(), wf.getPenaltyMaxY()).subtract(goal), new Vector2D(1, 0));

        List<IntegratedRobot> roleRobots = MathHelper.robotListFromIds(friendlyRobots, roleIds);

        // 優先的に壁にしたいロボットの指定
        final List<Integer> wallProperties = new ArrayList<>();

        Optional<IntegratedRobot> oppChaser = Optional.empty();
        Optional<Vector2D> oppChaserPos = Optional.empty();
        if (!oppositeRobots.isEmpty()) {
            // 敵chaserを求める
            Map<IntegratedRobot, Double> oppScoreList = new HashMap<>();
            for (IntegratedRobot robot : oppositeRobots.values()) {
                final Vector2D br = robot.getRobot().position().subtract(ballPos);
                oppScoreList.put(robot, (ballVel.dotProduct(MathHelper.normalized(br)) + 2000) / br.getNorm());
            }
            oppChaser = MathHelper.robotWithMaxScore(oppScoreList);
            if (oppChaser.isPresent()) {
                oppChaserPos = Optional.of(oppChaser.get().getRobot().position());
            }
        }

        this.prevWallTarget = this.wallTarget;

        // マークする敵ロボット
        List<IntegratedRobot> oppRobots = new ArrayList<>();
        for (Map.Entry<Integer, IntegratedRobot> robotEntry : oppositeRobots.entrySet()) {
            if (MathHelper.distancePositionToRobot(goal, robotEntry.getValue().getRobot()) < wf.getMaxY() + 1000.0
                    && MathHelper.distancePositionToRobot(ballPos, robotEntry.getValue().getRobot()) > 10 * ROBOT_RAD) {
                // ゴールに近くて、ボールに近すぎない敵
                oppRobots.add(robotEntry.getValue());
            }
        }

        List<Integer> tmpIds = new ArrayList<>(this.roleIds);

        // waller (壁) ///////////////////////
        {
            final boolean isBallSide =
                    Math.abs(MathHelper.direction(ballPos, goal)) > penaltyCornerTheta;

            // 壁の枚数
            int numWaller;
            if (this.wallSize.isPresent()) {
                numWaller = Math.min(this.wallSize.get(), tmpIds.size());
            } else {
                numWaller = Math.min(Math.clamp(tmpIds.size() - oppRobots.size(), 1, 5), tmpIds.size());
            }

            if (numWaller != 0 && numWaller <= tmpIds.size()) {
                // this.wallTarget（壁を作る目標）を求める
                if (oppChaserPos.isPresent()) {
                    // 基本的にはball,
                    // 敵chaserがパスを受けてシュートしそうであれば、ene_chaserを基準に壁を作る
                    this.wallTarget = ballVel.getNorm() > 800 && oppChaserPos.get().getX() < wf.getMinX() / 3
                            ? oppChaserPos.get()
                            : ballPos;
                } else {
                    this.wallTarget = ballPos;
                }

                final Vector2D goalMax = new Vector2D(wf.getMinX(), wf.getGoalMaxY() * 1.1);
                final Vector2D goalMin = new Vector2D(wf.getMinX(), wf.getGoalMinY() * 1.1);
                final double goalTheta1 =
                        MathHelper.wrap2PI(MathHelper.direction(goalMax, ballPos));
                final double goalTheta2 =
                        MathHelper.wrap2PI(MathHelper.direction(goalMin, ballPos));
                final double ballVel_theta = MathHelper.wrap2PI(MathHelper.direction(ballVel));

                // 敵のshootかどうか
                final boolean is_shoot =
                        goalTheta1 < ballVel_theta && ballVel_theta < goalTheta2 && ballVel.getNorm() > 1500;

                // 壁がボールより内側か
                final boolean is_wall_inside =
                        MathHelper.distance2D(this.wallCenter, goal) < MathHelper.distance2D(ballPos, goal) &&
                                this.wallCenter != Vector2D.ZERO;

                // 壁の中心を決める
                if (is_shoot && is_wall_inside) {
                    final double to_ball_theta = MathHelper.direction(ballPos, goal);
                    final boolean is_ball_front = Math.abs(to_ball_theta) < penaltyCornerTheta;
                    double magnification = is_ball_front
                            ? Math.abs((this.wallCenter.getX() - ballPos.getX()) / ballVel.getX())
                            : Math.abs((this.wallCenter.getY() - ballPos.getY()) / ballVel.getY());

                    // シュートだったらwall_centerのy座標をボールの進行方向上に
                    this.wallCenter = ballVel.scalarMultiply(magnification).add(ballPos);

                } else {
                    final double toTargetTheta = MathHelper.direction(this.wallTarget, goal);
                    final double dist_to_target = MathHelper.distance2D(this.wallTarget, goal);
                    final boolean is_side = Math.abs(toTargetTheta) > penaltyCornerTheta; // targetは横か

                    final double wall_radious_max = wf.getPenaltyMaxY() * 1.6; // 壁がゴールから離れる最大距離
                    // targetが遠ければ、ゴールから離れた円周上に
                    if (dist_to_target > wall_radious_max + 800) {
                        final double x = wf.getMinX() + Math.abs(wall_radious_max * Math.cos(toTargetTheta));
                        final double y = wall_radious_max * Math.sin(toTargetTheta);
                        this.wallCenter = new Vector2D(x, y);
                    } else {
                        // this.wallTargetの前
                        double x = wf.getMinX() + Math.abs((dist_to_target - 800) * Math.cos(toTargetTheta));
                        double y = (dist_to_target - 800) * Math.sin(toTargetTheta);
                        // penaltyの中に入っていたら外に出す
                        if ((x < wf.getPenaltyBackX() + 2 * ROBOT_RAD) && !is_side) {
                            y = y + (wf.getPenaltyBackX() + 2 * ROBOT_RAD - x) * Math.sin(toTargetTheta);
                            x = wf.getPenaltyBackX() + 2 * ROBOT_RAD;
                        }
                        if ((Math.abs(y) < wf.getPenaltyMaxY() + 2 * ROBOT_RAD) && is_side) {
                            y = Math.copySign(wf.getPenaltyMaxY() + 2 * ROBOT_RAD, this.wallTarget.getY());
                            x = y / Math.tan(toTargetTheta) + wf.getMinX();
                        }
                        if (x < wf.getMinX() + numWaller * ROBOT_RAD) x = wf.getMinX() + numWaller * ROBOT_RAD;
                        this.wallCenter = new Vector2D(x, y);
                    }
                }

                // ブロック中の壁ロボットの確認
                List<AbstractMap.SimpleEntry<Integer, WallerState>> blockingWallers = new ArrayList<>();
                for (final Map.Entry<Integer, WallerState> pair : this.wallers) {
                    for (final int id : tmpIds) {
                        if (id == pair.getKey() && pair.getValue() == WallerState.BLOCK) {
                            blockingWallers.add(new AbstractMap.SimpleEntry<>(pair.getKey(), pair.getValue()));
                            break;
                        }
                    }
                }

                // targetが切り替わったら, wallerをクリア
                if (MathHelper.distance2D(this.wallTarget, this.prevWallTarget) > 500) {
                    blockingWallers.clear();
                    this.wallers.clear();
                }

                // wallerを決める
                if (blockingWallers.size() > numWaller) {
                    // blockingWallersをnumWallerに切って使う
                    if(!wallProperties.isEmpty()){
                        blockingWallers.sort(InterfaceHelper.getComparator(new IFuncParam2<Boolean, AbstractMap.SimpleEntry<Integer, WallerState>, AbstractMap.SimpleEntry<Integer, WallerState>>() {
                            @Override
                            public Boolean function(AbstractMap.SimpleEntry<Integer, WallerState> a, AbstractMap.SimpleEntry<Integer, WallerState> b) {
                                // aは含まれていて、bは含まれていないなら並び替えない
                                if(wallProperties.contains(a.getKey()) && !wallProperties.contains(b.getKey())) return false;
                                // aは含まれておらず、bは含まれているなら並び替える
                                if(!wallProperties.contains(a.getKey()) && wallProperties.contains(b.getKey())) return true;
                                // 両方含まれているor両方含まれていないなら並び替えない
                                return false;
                            }
                        }));
                    }
                    this.wallers = blockingWallers.subList(0, numWaller);

                } else if (blockingWallers.size() < numWaller) {
                    // 不足していたら
                    this.wallers = blockingWallers;

                    // tmpIds から、存在しており、blockingWallersに含まれないものを候補に追加
                    // tmpIds は、start1秒後までは見えないidを含んでいるため
                    List<Integer> wallerCandidates = new ArrayList<>();
                    List<Integer> sortIds = new ArrayList<>();
                    for (final int id : tmpIds) {
                        if (!friendlyRobots.containsKey(id)) continue;
                        //ブロック中なら追加しない
                        boolean inBlock = false;
                        for (final AbstractMap.SimpleEntry<Integer, WallerState> pair : blockingWallers) {
                            if (pair.getKey() == id) {
                                inBlock = true;
                                break;
                            }
                        }
                        if(inBlock) continue;
                        // wall_properties_は優先して候補に、その他はsortするためにsort_idに代入
                        if(wallProperties.contains(id)) wallerCandidates.add(id);
                        else sortIds.add(id);
                    }

                    // robot_pos と this.wallCenter との距離が近い順にソート
                    sortIds.sort(InterfaceHelper.getComparator(new IFuncParam2<Boolean, Integer, Integer>() {
                        @Override
                        public Boolean function(Integer integer, Integer integer2) {
                            return MathHelper.distancePositionToRobot(wallCenter, friendlyRobots.get(integer).getRobot()) <
                                    MathHelper.distancePositionToRobot(wallCenter, friendlyRobots.get(integer2).getRobot());
                        }
                    }));

                    // sortしたものを候補に追加、台数を切って壁にする
                    wallerCandidates.addAll(sortIds);
                    wallerCandidates = new ArrayList<>(wallerCandidates.subList(0, numWaller - blockingWallers.size()));
                    for(final int id : wallerCandidates) {
                        this.wallers.add(new AbstractMap.SimpleEntry<>(id ,WallerState.MOVE));
                    }
                }

                // wallerをゴールからから見た角度で並べ替える
                this.wallers.sort(InterfaceHelper.getComparator(new IFuncParam2<Boolean, AbstractMap.SimpleEntry<Integer, WallerState>, AbstractMap.SimpleEntry<Integer, WallerState>>() {
                    @Override
                    public Boolean function(AbstractMap.SimpleEntry<Integer, WallerState> a, AbstractMap.SimpleEntry<Integer, WallerState> b) {
                        return MathHelper.direction(friendlyRobots.get(a.getKey()).getRobot().position(), goal) >
                                MathHelper.direction(friendlyRobots.get(b.getKey()).getRobot().position(), goal);
                    }
                }));
                for (final AbstractMap.SimpleEntry<Integer, WallerState> pair : this.wallers) {
                    tmpIds.remove(pair.getKey());
                }
                for (final AbstractMap.SimpleEntry<Integer, WallerState> pair : this.wallers) {
                    tmpIds.removeIf(InterfaceHelper.getPredicate(new IFuncParam1<Boolean, Integer>() {
                        @Override
                        public Boolean function(Integer integer) {
                            return (int)integer == pair.getKey();
                        }
                    }));
                }
            } else {
                this.wallTarget = Vector2D.ZERO;
                this.wallCenter = Vector2D.ZERO;
                this.wallers.clear();
            }
        }
        if (!this.wallers.isEmpty()) {
            final double toTargetTheta = MathHelper.direction(this.wallTarget, goal);
            final boolean isBallSide = toTargetTheta > penaltyCornerTheta;

            // block状態にあるwallerの数
            int  numFormedWaller = 0;
            for (final AbstractMap.SimpleEntry<Integer, WallerState> waller : this.wallers) {
                if (waller.getValue() == WallerState.BLOCK) numFormedWaller++;
            }

            // 目標座標を設定
            // 先にblock_posを設定し、その両端に現在の角度を考慮してmove_posを設定する
            // ロボットが集まっていない状態でも使えるrobotだけでブロックできるようこの形とした
            final double correction =
                    (!isBallSide && numFormedWaller >= 3) ? 0.8 : 1; // 台数が多いときは傾きを抑える
            final Vector2D wallShift = MathHelper.applyRotation2D(new Vector2D(0, 1), correction * toTargetTheta).scalarMultiply(ROBOT_RAD);

            List<Vector2D> blockPos = new ArrayList<>();
            for (int i = 0; i < numFormedWaller; i++) {
                final int baseShiftNum = (numFormedWaller) - 1;
                final int shiftNum = baseShiftNum - (2 * i);
                blockPos.add(this.wallCenter.add(wallShift.scalarMultiply(shiftNum)));
            }

            List<Vector2D> movePos = new ArrayList<>();
            int frontCount = 1, backCount = 1;
            for (final AbstractMap.SimpleEntry<Integer, WallerState> pair : this.wallers) {
                if (pair.getValue() == WallerState.BLOCK) continue;
                double bot_theta = MathHelper.direction(friendlyRobots.get(pair.getKey()).getRobot().position(), goal);
                if (bot_theta > toTargetTheta) {
                    Vector2D basePos = blockPos.isEmpty() ? this.wallCenter : blockPos.getFirst();
                    Vector2D shift = wallShift.scalarMultiply(1.2 * frontCount);
                    movePos.addFirst(basePos.add(shift));
                    frontCount += 2;
                } else {
                    Vector2D basePos = blockPos.isEmpty() ? this.wallCenter : blockPos.getLast();
                    Vector2D shift = wallShift.scalarMultiply(1.2 * backCount);
                    movePos.add(basePos.subtract(shift));
                    backCount += 2;
                }
            }

            for (AbstractMap.SimpleEntry<Integer, WallerState> waller : this.wallers) {
                final Integer id = waller.getKey();
                final WallerState state = waller.getValue();

                final Vector2D wallerPos = friendlyRobots.get(id).getRobot().position();
                final boolean isBallFront =
                        MathHelper.distance2D(ballPos, wallerPos) < 500 &&
                                MathHelper.direction(ballPos, wallerPos) - friendlyRobots.get(id).getRobot().getTheta() <
                        Math.PI / 3;

                // 障害物
                List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, id, CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot());
                if (avoidBall) {
                    obstacles.add(new ObstacleCircle(ballPos, 500, 100)); // ボールを避ける
                }
                if (this.ballPlacePos.isPresent()) {
                    obstacles.add(new ObstacleSegment(ballPos, this.ballPlacePos.get(), 650));
                }
                switch (state) {
                    case WallerState.BLOCK:
                        this.guardMap.get(id).setPos(blockPos.getFirst());
                        this.guardMap.get(id).setAngle(toTargetTheta);
                        if (isBallFront) {
                            // this.guardMap.get(id).setDribble(5);
                            // this.guardMap.get(id).setKickType({model::command::kick_type_t::chip, 230});
                        }
                        if (this.avoidBall || this.ballPlacePos.isPresent()) {
                            list.add(new WithPlanner<>(guardMap.get(id), id, this.color, obstacles));
                        } else {
                            list.add(guardMap.get(id));
                        }
                        if (MathHelper.distance2D(wallerPos, blockPos.getFirst()) > 500) {
                            waller.setValue(WallerState.MOVE);
                        }
                        blockPos.removeFirst();
                        break;

                    case WallerState.MOVE:
                        this.moveMap.get(id).setPos(movePos.getFirst());
                        this.moveMap.get(id).setAngle(toTargetTheta);
                        list.add(new WithPlanner<>(moveMap.get(id), id, this.color, obstacles));
                        if (MathHelper.distancePositionToRobot(movePos.getFirst(), friendlyRobots.get(id).getRobot()) < 300) {
                            waller.setValue(WallerState.BLOCK);
                        }
                        movePos.removeFirst();
                        break;
                }
            }
        }

        // 壁以外のディフェンス
        {
            List<Vector2D> waitPositions = new ArrayList<>(Arrays.asList(
                    new Vector2D(wf.getPenaltyBackX() + 500, 0),
                    new Vector2D(wf.getPenaltyBackX() + 500, wf.getPenaltyMaxY() + 500),
                    new Vector2D(wf.getPenaltyBackX() + 500, wf.getPenaltyMinY() - 500),
                    new Vector2D(wf.getPenaltyBackX() + 500, 1000),
                    new Vector2D(wf.getPenaltyBackX() + 500, -1000),
                    new Vector2D(wf.getPenaltyBackX(), wf.getPenaltyMaxY() + 500),
                    new Vector2D(wf.getPenaltyBackX(), wf.getPenaltyMinY() - 500),
                    new Vector2D(wf.getPenaltyBackX() + 1000, 0),
                    new Vector2D(wf.getPenaltyBackX() + 1000, 500),
                    new Vector2D(wf.getPenaltyBackX() + 1000, -500)
            ));

            Map<IntegratedRobot, Double> markScoreMap = new HashMap<>();
            for (IntegratedRobot robot : oppRobots) {
                // x座標が小さいものの優先順位を高く
                markScoreMap.put(robot, -1 * robot.getRobot().getX());
            }
            List<IntegratedRobot> markRobotList = MathHelper.sortInGreaterScore(markScoreMap);
            List<Vector2D> markPositions = new ArrayList<>();
            for (IntegratedRobot markRobot : markRobotList) {
                if (waitPositions.isEmpty()) break;
                Vector2D nearest = MathHelper.nearestPositionToRobot(waitPositions, markRobot).get();
                Vector2D robotPosition = markRobot.getRobot().position();
                markPositions.add(robotPosition.add(MathHelper.normalized(ballPos.subtract(robotPosition)).scalarMultiply(ROBOT_RAD * 3)));
                waitPositions.remove(nearest);
            }
            waitPositions.addAll(0, markPositions);

            List<IntegratedRobot> tmpRobots = new ArrayList<>(MathHelper.robotListFromIds(friendlyRobots, tmpIds));

            Map<Integer, Vector2D> targetPositions = new HashMap<>();

            for (Vector2D position : waitPositions) {
                if (tmpRobots.isEmpty()) {
                    break;
                }
                IntegratedRobot nearest = MathHelper.nearestRobotToPosition(tmpRobots, position).get();
                targetPositions.put(nearest.getId(), position);
                tmpRobots.remove(nearest);
            }

            for (int id : tmpIds) {
                // 障害物
                List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, id, CommonObstacles.getMarginRobot(), this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot());
                if (avoidBall) {
                    obstacles.add(new ObstacleCircle(ballPos, 500, 100)); // ボールを避ける
                }
                if (this.ballPlacePos.isPresent()) {
                    obstacles.add(new ObstacleSegment(ballPos, this.ballPlacePos.get(), 650));
                }
                this.moveMap.get(id).setPos(targetPositions.get(id));
                this.moveMap.get(id).setAngle(MathHelper.direction(ballPos, friendlyRobots.get(id).getRobot().position()));
                list.add(new WithPlanner<>(moveMap.get(id), id, this.color, obstacles));
            }
        }

        return list;
    }

    @Override
    public String getName() {
        return "role_defense";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setAvoidBall(boolean avoidBall) {
        this.avoidBall = avoidBall;
    }

    public void setBallPlacePos(Optional<Vector2D> ballPlacePos) {
        this.ballPlacePos = ballPlacePos;
    }

    /**
     * 壁の台数を設定する
     * @param size 壁の台数（emptyなら自動）
     */
    public void setWallSize(Optional<Integer> size) {
        this.wallSize = size;
    }
}