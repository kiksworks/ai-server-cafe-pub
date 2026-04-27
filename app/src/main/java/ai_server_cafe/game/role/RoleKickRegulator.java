package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionBallPlace;
import ai_server_cafe.game.action.ActionKick;
import ai_server_cafe.game.action.ActionMove;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.KickConverter;
import ai_server_cafe.util.math.MathHelper;
import ai_server_cafe.util.writer.WriterExternal;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleKickRegulator extends AbstractRole {
    public static final double THRESHOLD = 300.0;
    public static final int CHANGE_VALUE = 30;
    public static final int INIT_POWER = 40;
    public static final int MAX_POWER = 180;

    public enum RegulatorState {
        KICK,          /// ボールを蹴る
        HALT,          /// 停止
        WAIT,          /// 測定後、BALL_PLACEかKICKか考える
        BALL_PLACE,    /// ボールを置く
        BALL_PASS,     /// ボールをパスする
        BALL_RECEIVE,  /// ボールを受け取る(BALL_PASS後の状態遷移)
        MEASURE,       /// 測定
        MEASURE_FAILED /// キックパワーが弱すぎて蹴らない，
    }

    private boolean isSecond;
    private boolean isFinished;
    private RegulatorState state;
    private Vector2D[] initPos = new Vector2D[]{new Vector2D(0.0, -2500.0), new Vector2D(0.0, 2500.0)};
    private int power;
    private final boolean isGrSimMode;
    private final double grSimKickSpeed;
    private final WriterExternal external;
    private double maxSpeed;
    private final Map<Integer, List<Pair<Integer, Double>>> result;
    private final Map<Integer, Boolean> regulatorMap;
    private boolean isBallFast;
    // ボール減速度バッファ
    private List<Double> ballBrakeBuffer;
    // 前フレームのボール速度
    private Vector2D lastBallVel;

    private static final Logger LOGGER = LogManager.getLogger("kick-regulator");

    public RoleKickRegulator(TeamColor color, int[] ids) {
        super(color, ids);
        this.isSecond = false;
        this.isFinished = false;
        this.state = RegulatorState.HALT;
        this.power = INIT_POWER;
        this.isGrSimMode = false;
        this.grSimKickSpeed = 4000;
        this.external = new WriterExternal("grSimKickData-" + this.grSimKickSpeed + ".csv", null, StandardCharsets.UTF_8);
        this.maxSpeed = 0;
        this.result = new HashMap<>();
        this.regulatorMap = new HashMap<>();
        this.isBallFast = false;
        this.ballBrakeBuffer = new ArrayList<>();
        this.lastBallVel = Vector2D.ZERO;
        for(int id = 0; id < ConfigManager.MAX_ROBOTS; id++) {
            this.regulatorMap.put(id, Boolean.FALSE);
            this.result.put(id, new ArrayList<>(List.of(new Pair<>(0, 0.0))));
        }
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty() || this.roleIds.isEmpty()) return list;
        final int[] ids;

        final boolean isSoloRegulator = (this.roleIds.size() == 1 || this.roleIds.getFirst().equals(this.roleIds.get(1)));
        if(isSoloRegulator) {
            this.isSecond = false;
            ids = new int[]{this.roleIds.getFirst(), this.roleIds.getFirst()};
        } else {
            ids = new int[]{this.roleIds.getFirst(), this.roleIds.get(1)};
        }
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        if (!getVisibleIds(this.world.get(), this.color, this.activeRobots).contains(ids[0]) ||
                !getVisibleIds(this.world.get(), this.color, this.activeRobots).contains(ids[1])) return list;
        final FilteredBall ball = this.world.get().getBall();
        final Map<Integer, IntegratedRobot> friendlyRobotMap = this.world.get().getFriendlyRobotMap(this.color);

        final boolean isBallStopping = (ball.velocity().getNorm() <= THRESHOLD);

        // isSecondがtrueのとき1、falseのとき0になる
        // ids[sgn]はキッカー、ids[1 - sgn]はサポーターを指す
        final int sgn = ((this.isSecond && !isSoloRegulator) ? 1 : 0);
        final IntegratedRobot leader = friendlyRobotMap.get(ids[sgn]);
        final IntegratedRobot supporter = friendlyRobotMap.get(ids[1 - sgn]);
        final Vector2D[] waitPositions;
        if(!isSoloRegulator && this.regulatorMap.get(ids[1])) {
            // 相方がregulator済の場合
            waitPositions = new Vector2D[]{
                    new Vector2D(this.initPos[0].getX(), this.initPos[0].getY()),
                    new Vector2D(this.initPos[1].getX(), this.initPos[1].getY() * 0.75)};
        } else if(this.power < 80) {
            // 力が弱い場合
            waitPositions = new Vector2D[]{
                    new Vector2D(this.initPos[0].getX(), this.initPos[0].getY() * 0.75),
                    new Vector2D(this.initPos[1].getX(), this.initPos[1].getY() * 0.75)};
        } else {
            waitPositions = new Vector2D[]{
                    new Vector2D(this.initPos[0].getX(), this.initPos[0].getY()),
                    new Vector2D(this.initPos[1].getX(), this.initPos[1].getY())};
        }

        // 終わったらHALT
        if (this.isFinished) {
            this.state = RegulatorState.HALT;
            this.power = INIT_POWER;
            for (int id : ids) this.regulatorMap.put(id, Boolean.TRUE);
        } else {
            // 状態遷移
            switch (this.state) {
                case HALT : {
                    this.state = RegulatorState.WAIT;
                    break;
                }
                case WAIT : {
                    this.isBallFast = false;
                    if (!isSoloRegulator && MathHelper.distancePositionToRobot(
                            waitPositions[1 - sgn], supporter.getRobot()) > 6 * ROBOT_RAD)
                        break;
                    if (MathHelper.distance2D(waitPositions[sgn], ball.position()) > 5 * ROBOT_RAD) {
                        this.state = RegulatorState.BALL_PLACE;
                    } else if (MathHelper.distancePositionToRobot(
                            ball.position(), leader.getRobot()) > 2 * ROBOT_RAD
                            && ball.velocity().getNorm() < 100.0) {
                        this.state = RegulatorState.KICK;
                    }
                    break;
                }
                case BALL_PLACE : {
                    if (this.ballPlaceMap.get(ids[sgn]).isFinished()
                            && (isSoloRegulator || MathHelper.distancePositionToRobot(
                            waitPositions[1 - sgn], supporter.getRobot()) < 6 * ROBOT_RAD)) {
                        this.state = RegulatorState.KICK;
                    }
                    break;
                }
                case KICK : {
                    if (!isBallStopping) {
                        this.state = RegulatorState.MEASURE;
                    } else if (MathHelper.distance2D(ball.position(), waitPositions[sgn]) > 1600) {
                        // ボールを蹴らなかった場合
                        this.state = RegulatorState.MEASURE_FAILED;
                    }
                    break;
                }
                case BALL_PASS : {
                    if(isSoloRegulator) {
                        this.state = RegulatorState.WAIT;
                        this.maxSpeed = 0;
                    }
                    if (FastMath.abs(ball.position().getY())
                            < 0.75 * this.world.get().getField().getMaxY()
                            && FastMath.abs(ball.getY() - supporter.getRobot().getY()) < 2 * ROBOT_RAD) {
                        // ボールを蹴る隙間があるか
                        this.state = RegulatorState.BALL_RECEIVE;
                    }
                    break;
                }
                case BALL_RECEIVE:
                    if (isBallStopping && MathHelper.distancePositionToRobot(ball.position(), leader.getRobot())
                            < MathHelper.distancePositionToRobot(ball.position(), supporter.getRobot())) {
                        this.state = RegulatorState.WAIT;
                        this.maxSpeed = 0;
                    }
                    break;
                case MEASURE_FAILED :
                    this.result.get(ids[sgn]).add(new Pair<>(0, 0.0));
                case MEASURE : {
                    if (isBallStopping) {
                        if (!isSoloRegulator && this.regulatorMap.get(ids[1 - sgn])) {
                            // 次のロボットがregulator済みの場合
                            // supporterの方がボールに近かったら、leaderにボールを蹴ってあげる
                            this.state = (FastMath.abs(ball.getY() - supporter.getRobot().getY())
                                    < FastMath.abs(ball.getY() - leader.getRobot().getY())) ?
                                    RegulatorState.BALL_PASS : RegulatorState.BALL_PLACE;
                        } else {
                            // 次のロボットがregulatorしていない場合
                            this.state = RegulatorState.WAIT;
                            this.isSecond = !this.isSecond;
                        }
                    }
                    if (!this.isBallFast) {
                        this.isBallFast = (ball.velocity().getNorm() > 1500);
                    }

                    // 記録
                    if (!this.isGrSimMode && this.state.equals(RegulatorState.MEASURE)) {
                        if (!isBallStopping && MathHelper.distancePositionToRobot(ball.position(),
                                leader.getRobot()) < 2500.0) {
                            // ボールの最大速度を記録(蹴られた直後は記録しない)
                            this.maxSpeed = FastMath.max(ball.velocity().getNorm(), this.maxSpeed);
                        } else if (this.maxSpeed != 0 && (isBallStopping
                                || MathHelper.distancePositionToRobot(
                                        ball.position(), leader.getRobot()) >= 2500.0)) {
                            // ボールを蹴った後、最大速度を記録
                            final double speed = convertToCommandVel(this.maxSpeed);
                            final int currentPower = this.power;
                            if (this.result.get(ids[sgn]).stream().noneMatch(InterfaceHelper.getPredicate(
                                    new IFuncParam1<Boolean, Pair<Integer, Double>>() {
                                        @Override
                                        public Boolean function(Pair<Integer, Double> pair) {
                                            return pair.getFirst() == currentPower || pair.getSecond() > speed;
                                        }
                                    }))) this.result.get(ids[sgn]).add(new Pair<>(currentPower, speed));
                            this.maxSpeed = 0.0;
                        }
                    }

                    if (isBallStopping && (isSoloRegulator || !this.isSecond || this.regulatorMap.get(ids[1 - sgn])))
                        this.power += CHANGE_VALUE;
                    this.isFinished = (this.power > MAX_POWER);
                    break;
                }
            }

            // grSimModeにおける記録
            if (this.isGrSimMode && this.isFinished) {
                this.external.append(MathHelper.distancePositionToRobot(ball.position(), friendlyRobotMap.get(ids[0]).getRobot())
                        + "," + ball.velocity().getNorm() + "\n");
                this.external.append(MathHelper.distancePositionToRobot(ball.position(), friendlyRobotMap.get(ids[1]).getRobot())
                        + "," + ball.velocity().getNorm() + "\n");
                if (isBallStopping) {
                    this.external.close();
                }
            }
        }

        // 制御
        this.visualizerTargets = new ArrayList<>();
        ActionKick kick;
        ActionMove move;
        ActionBallPlace place;
        // supporterが向く角度
        final double supporterAngle = MathHelper.direction(waitPositions[sgn], waitPositions[1 - sgn]);
        switch (this.state) {
            case WAIT : {
                move = this.moveMap.get(ids[sgn]);
                move.setPos(ball.position()
                        .add(MathHelper.normalized(waitPositions[sgn].subtract(waitPositions[1 - sgn]))
                                .scalarMultiply(ROBOT_RAD * 3)));
                move.setAngle(supporterAngle + Math.PI);
                list.add(move);

                if(isSoloRegulator) break;
                move = this.moveMap.get(ids[1 - sgn]);
                move.setPos(waitPositions[1 - sgn]);
                move.setAngle(supporterAngle);
                list.add(move);
                break;
            }
            case BALL_PLACE : {
                this.visualizerTargets.add(waitPositions[sgn]);
                place = this.ballPlaceMap.get(ids[sgn]);
                place.setAbpTarget(waitPositions[sgn]);
                list.add(place);

                if(isSoloRegulator) break;
                move = this.moveMap.get(ids[1 - sgn]);
                move.setPos(waitPositions[1 - sgn]);
                move.setAngle(supporterAngle);
                list.add(move);
                break;
            }
            case KICK : {
                final Vector2D kickTarget = this.initPos[1 - sgn];
                this.visualizerTargets.add(kickTarget);

                kick = this.kickMap.get(ids[sgn]);
                kick.kickManually(new Pair<>(EnumKickType.STRAIGHT, !this.isGrSimMode ? this.power :
                        KickConverter.toPower(ids[sgn], new Pair<>(EnumKickType.STRAIGHT, this.grSimKickSpeed))));
                kick.setTarget(kickTarget);
                kick.setIsSetPlay(true);
                kick.setHoldTime(30);
                kick.setPushSpeed(300);
                list.add(kick);

                if(isSoloRegulator) break;
                move = this.moveMap.get(ids[1 - sgn]);
                move.setPos(waitPositions[1 - sgn]);
                move.setAngle(supporterAngle);
                list.add(move);
                break;
            }
            case HALT : {
                list.add(this.haltMap.get(ids[0]));
                list.add(this.haltMap.get(ids[1]));
                return list;
            }
            case MEASURE_FAILED :
            case MEASURE : {
                move = this.moveMap.get(ids[sgn]);
                move.setPos(waitPositions[sgn]);
                move.setAngle(supporterAngle + Math.PI);
                list.add(move);

                if(isSoloRegulator) break;
                if (this.regulatorMap.get(ids[1 - sgn]) && !this.isBallFast
                        || (this.power + CHANGE_VALUE >= MAX_POWER && this.isSecond)) {
                    // 次のロボットがregulator済みで、leaderの蹴る力が弱い場合
                    // 今のキックで終わる時はボールを追わない
                    move = this.moveMap.get(ids[1 - sgn]);
                    move.setPos(waitPositions[1 - sgn]);
                    move.setAngle(supporterAngle);
                    list.add(move);
                } else {
                    list.add(this.receiveMap.get(ids[1 - sgn]));
                }
                break;
            }
            case BALL_PASS : {
                this.visualizerTargets.add(leader.getRobot().position());
                move = this.moveMap.get(ids[sgn]);
                move.setPos(this.initPos[sgn]);
                move.setAngle(MathHelper.direction(ball.position(), leader.getRobot().position()));
                list.add(move);

                place = this.ballPlaceMap.get(ids[1 - sgn]);
                place.setAbpTarget(leader.getRobot().position());
                list.add(place);
                break;
            }
            case BALL_RECEIVE : {
                this.visualizerTargets.add(leader.getRobot().position());

                list.add(this.receiveMap.get(ids[sgn]));

                if (isBallStopping) {
                    kick = this.kickMap.get(ids[1 - sgn]);
                    kick.kickManually(KickConverter.toPower(ids[1 - sgn],
                            new Pair<>(EnumKickType.STRAIGHT, KickConverter.passSpeed(
                                    supporter.getRobot().position(), leader.getRobot().position()))));
                    kick.setTarget(leader.getRobot().position());
                    kick.setIsSetPlay(true);
                    kick.setHoldTime(30);
                    list.add(kick);
                } else {
                    move = this.moveMap.get(ids[1 - sgn]);
                    move.setPos(waitPositions[1 - sgn]);
                    move.setAngle(supporterAngle);
                    list.add(move);
                }
                break;
            }
        }

        // ボール減速度計算
        if (this.state.equals(RegulatorState.MEASURE)
                && MathHelper.inferiorAngle(ball.velocity(), this.lastBallVel) < 0.1 * Math.PI) {
            Vector2D ballBrake = (ball.velocity().subtract(this.lastBallVel))
                    .scalarMultiply(1 / ConfigManager.getInstance().getCycleTime());
            this.ballBrakeBuffer.addLast(ballBrake.getNorm());
            if (this.ballBrakeBuffer.size() >= 120) {
                // データを120個とったら中央値求める
                List<Double> sortedBuff = this.ballBrakeBuffer.stream().sorted().toList();
                double median = sortedBuff.get(60);
                double lastBallBrake = ConfigManager.getInstance().getConfig().ballBrake;
                if (median > 100 && Math.abs((median - lastBallBrake) / median) > 0.005) {
                    // 誤差率が0.5%より大きいとき
                    // コンフィグ書き換え
                    ConfigManager.getInstance().getEditableConfig().ballBrake = median;
                    LOGGER.info("ball brake config changed: {} -> {}",
                            String.format("%.2f", lastBallBrake), String.format("%.2f", median));
                }
                // バッファさよなら
                this.ballBrakeBuffer.clear();
            }
        }
        this.lastBallVel = ball.velocity();

        // 障害物の設定 (ball, ロボットを追加)
        List<AbstractAction> returnList = new ArrayList<>();
        for(AbstractAction action : list) {
            int id = action.getId();
            List<AbstractObstacle> obstacles = new ArrayList<>();
            if(!this.state.equals(RegulatorState.BALL_PLACE) && id == leader.getId()) {
                // PLACE時以外は仲間のロボットを避ける
                obstacles.addAll(CommonObstacles.getFriendlyRobotsIdsExcluded(this.world.get(), this.color,
                        new int[]{id}, friendlyRobotMap.get(id).getRobot(), CommonObstacles.getMarginRobot()));
            }
            obstacles.addAll(CommonObstacles.getOppositeRobots(this.world.get(), this.color,
                    friendlyRobotMap.get(id).getRobot(), CommonObstacles.getMarginRobot()));
            if(!isSoloRegulator && this.state.equals(RegulatorState.BALL_PLACE) && id == supporter.getId()) {
                // PLACE時，サポーターはボールを避ける
                obstacles.add(new ObstacleCircle(ball.position(), 500.0, CommonObstacles.getMarginRobot()));
            }
            returnList.add(new WithPlanner<>(action, id, this.color, obstacles));
        }
        return returnList;
    }

    @Override
    public String getName() {
        return "role_kick_regulator";
    }

    @Override
    public boolean isFinished() {
        return this.isFinished;
    }

    public boolean isAllFinished(List<Integer> ids) {
        for(Integer id : ids) {
            if(!this.regulatorMap.get(id)) {
                return false;
            }
        }
        return true;
    }

    public void reset() {
        this.isFinished = false;
        this.isSecond = false;
        this.power = INIT_POWER;
        this.state = RegulatorState.BALL_PLACE;
    }

    public void setInitPosition(Vector2D initPos1, Vector2D initPos2) {
        this.initPos = new Vector2D[]{initPos1, initPos2};
    }

    private static double convertToCommandVel(double detectedSpeed) {
        // GrSimによる実験値
        return -0.00002 * detectedSpeed * detectedSpeed + 1.244 * detectedSpeed + 222.69;
    }

    /**
     *
     * @return ballをパワー(Integer)で蹴ったときのballの最高速度(Double)
     */
    public Map<Integer, List<Pair<Integer, Double>>> getResult() {
        return this.result;
    }
}
