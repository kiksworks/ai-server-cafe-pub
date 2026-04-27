package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionBallPlace;
import ai_server_cafe.game.action.ActionDribble;
import ai_server_cafe.game.action.ActionGetBall;
import ai_server_cafe.game.action.ActionKick;
import ai_server_cafe.game.action.ActionMitoma;
import ai_server_cafe.game.action.ActionMove;
import ai_server_cafe.game.action.ActionReceive;
import ai_server_cafe.game.action.ActionRobBall;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.base.ObstacleSegment;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterScoreBoard;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.game.StrategyHelper;
import ai_server_cafe.util.math.KickConverter;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoleDemo extends AbstractRole {
    private Map<Integer, ActionDribble> dribbleMap;

    public enum RunningState {
        NONE, GETBALL, DRIBBLE, DEFENSE, SHOOT, STOPGAME, MITOMA
    }

    private RunningState state;
    private boolean haveBall;
    private boolean isOppShoot;
    private double defenseLine;
    private int lostBallCount;
    private boolean finished;
    private int lastScoreYellow;
    private int lastScoreBlue;

    public RoleDemo(TeamColor color, int[] ids) {
        super(color, ids);
        this.state = RunningState.NONE;
        this.dribbleMap = new HashMap<>();
        this.haveBall = false;
        this.defenseLine = 0;
        this.lostBallCount = 0;
        this.finished = false;
        for (int id : ids) {
            this.dribbleMap.put(id, new ActionDribble(id, color));
        }
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty()) {
            return list;
        }
        this.visualizerTargets = new ArrayList<>();
        if (this.roleIds.isEmpty()) {
            return list;
        }
        final Integer id = this.roleIds.getFirst();
        final FilteredBall ball = this.world.get().getBall();
        final FilteredRobot robot = this.world.get().getFriendlyRobotMap(color).get(id).getRobot();
        final Optional<IntegratedRobot> oppRobot = MathHelper
                .nearestRobotToPosition(this.world.get().getOppositeRobotMap(color), ball.position());

        Vector2D ballPos = ball.position();
        final Vector2D ballVel = ball.velocity();
        final Vector2D robotPos = robot.position();
        Optional<Vector2D> oppRobotPos = Optional.empty();
        Vector2D oppRobotVel = Vector2D.ZERO;
        if (oppRobot.isPresent()) {
            oppRobotPos = Optional.of(oppRobot.get().getRobot().position());
            oppRobotVel = oppRobot.get().getRobot().velocity();
        }

        final double distBtoR = MathHelper.distance2D(robotPos, ballPos);
        Optional<Double> distBtoO = Optional.empty();
        if (oppRobot.isPresent())
            distBtoO = Optional.of(MathHelper.distance2D(oppRobotPos.get(), ballPos));
        final Field wf = this.world.get().getField();
        final Vector2D goalFront = new Vector2D(wf.getGoalFrontX() - wf.getGoalLength(), 0);
        final Vector2D goalBack = new Vector2D(wf.getGoalBackX() + wf.getGoalLength(), 0);
        final Vector2D goalWidth = new Vector2D(0, wf.getGoalWidth() / 2);
        final Vector2D goalLength = new Vector2D(wf.getGoalLength(), 0);
        final int aiLevel = ConfigManager.getInstance().getConfig().demo.isDifficultyAuto
                ? UpdaterScoreBoard.getInstance().getAiLevel(this.color) : ConfigManager.getInstance().getConfig().demo.difficulty;
        double penaltyLine = switch (aiLevel) {
            case 0 -> 0;
            case 1 -> wf.getGameWidth() / 4;
            default -> wf.getGameWidth() / 2;
        };
        final Vector2D penaltyCenter = new Vector2D(penaltyLine, 0);

        Vector2D movePos;
        // ロボット半径
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        // ボール半径
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        // ロボット中心からキッカー表面までの距離
        final double TO_FACE_RAD = ConfigManager.getInstance().getConfig().toFaceRadius;
        // 制御周期
        final double cycle = ConfigManager.getInstance().getConfig().getCycleTime();

        final Vector2D faceBallPos = robotPos.add(MathHelper.getFromPolar(TO_FACE_RAD + BALL_RAD, robot.getTheta()));
        Vector2D target = goalFront;
        List<Vector2D> shootTargets = new ArrayList<>();
        StrategyHelper.findShootTarget(this.world.get(), this.color, robotPos);
        if (!shootTargets.isEmpty()) {
            target = StrategyHelper.sortShootTarget(this.world.get(), this.color,
                    shootTargets, robotPos, robot.getTheta()).getFirst();
        }

        boolean avoidBall = false;

        final boolean isBallWall = (FastMath.abs(ballPos.getX()) + ROBOT_RAD * 2 > wf.getGameWidth() / 2
                || FastMath.abs(ballPos.getY()) + ROBOT_RAD * 2 > wf.getGameHeight() / 2)
                && (FastMath.abs(ballPos.getY()) > goalWidth.getY());

        if (this.haveBall && this.world.get().getBall().isLost())
            ballPos = faceBallPos;
        if (haveBall) {
            if (MathHelper.inferiorAngle(robot.getTheta(), MathHelper.direction(ballPos, robotPos)) > 0.4 ||
                    distBtoR > 200)
                lostBallCount++;
            else
                lostBallCount = 0;
            if (lostBallCount >= 10)
                haveBall = false;
        } else {
            haveBall = (MathHelper.inferiorAngle(robot.getTheta(),
                    MathHelper.direction(ballPos, robotPos)) < 0.2 &&
                    MathHelper.distance2D(ballPos, faceBallPos) < TO_FACE_RAD + BALL_RAD + 10);
            lostBallCount = 0;
        }
        UpdaterWorld.getInstance().setHaveBall(haveBall, color);

        final double canShootLine = FastMath.min(goalFront.getX() - 2000, penaltyLine - 500);
        final boolean canShoot = canShootLine < ballPos.getX() || (oppRobot.isEmpty()
                || robotPos.getX() > oppRobotPos.get().getX());

        if (isOppShoot) {
            isOppShoot = ballVel.getNorm() > 300
                    && (MathHelper.wrap2PI(MathHelper.direction(ballVel)) > MathHelper
                    .wrap2PI(MathHelper.direction(goalBack.add(goalWidth).subtract(ballPos)))
                    && MathHelper.wrap2PI(MathHelper.direction(ballVel)) < MathHelper
                    .wrap2PI(MathHelper.direction(goalBack.subtract(goalWidth).subtract(ballPos))));
        } else {
            isOppShoot = (ballVel.getX() < -800
                    && MathHelper.wrap2PI(MathHelper.direction(ballVel)) > MathHelper
                    .wrap2PI(MathHelper.direction(goalBack.add(goalWidth).subtract(ballPos)))
                    && MathHelper.wrap2PI(MathHelper.direction(ballVel)) < MathHelper.wrap2PI(MathHelper
                    .direction(goalBack.subtract(goalWidth).subtract(ballPos))));
        }
        if (ConfigManager.getInstance().getConfig().useScoreBoard) {
            if (UpdaterScoreBoard.getInstance().getScoreYellow() > lastScoreYellow ||
                    UpdaterScoreBoard.getInstance().getScoreBlue() > lastScoreBlue) {
                finished = true;
                lastScoreYellow = UpdaterScoreBoard.getInstance().getScoreYellow();
                lastScoreBlue = UpdaterScoreBoard.getInstance().getScoreBlue();
            }
            if (finished && Math.abs(ballPos.getX()) < wf.getMaxX() - ROBOT_RAD * 2
                    || (oppRobot.isPresent() && MathHelper.distance2D(ballPos, oppRobotPos.get()) < 100)) {
                finished = false;
            }
        }

        // 動作の決定
        if (finished) {
            this.state = RunningState.STOPGAME;
        } else if (isOppShoot || (oppRobot.isPresent() && distBtoR > distBtoO.get() + 100
                && distBtoO.get() < 150) || ballPos.getX() > penaltyLine) {
            this.state = RunningState.DEFENSE;
        } else if (isBallWall) {
            this.state = RunningState.GETBALL;
        } else if (haveBall) {
            if (canShoot) {
                // shoot
                this.state = RunningState.SHOOT;
                // オーバードリブルで蹴らないのを防ぐ
                UpdaterWorld.getInstance().setHaveBall(false, color);
                UpdaterWorld.getInstance().setHaveBall(true, color);
            } else if (robotPos.getX() < canShootLine - 2000
                    && MathHelper.inferiorAngle(MathHelper.direction(oppRobotPos.get().subtract(robotPos)),
                    robot.getTheta()) > FastMath.PI * 0.5) {
                // mitoma
                this.state = RunningState.MITOMA;
            } else {
                // dribble
                this.state = RunningState.DRIBBLE;
            }
        } else {
            this.state = RunningState.GETBALL;
        }

        // 動作
        switch (this.state) {
            case RunningState.GETBALL: {
                if (isBallWall && FastMath.abs(ballPos.getX()) < wf.getMaxX() - ROBOT_RAD * 3) {
                    // 壁キック
                    ActionKick kick = kickMap.get(id);
                    kick.setIsSetPlay(true);
                    kick.setTarget(ballPos.add(new Vector2D(1000, FastMath.copySign(1000, ballPos.getY()))));
                    kick.kickManually(new Pair<>(EnumKickType.STRAIGHT,
                            KickConverter.toPower(id, new Pair<>(EnumKickType.STRAIGHT, 3500.0))));
                    list.add(kick);
                } else if (isBallWall) {
                    // ボールが壁付近にあるとき
                    ActionBallPlace ballPlace = ballPlaceMap.get(id);
                    ballPlace.setAbpTarget(goalFront);
                    list.add(ballPlace);
                } else if (oppRobot.isPresent() && distBtoO.get() < 150) {
                    ActionRobBall robBall = robBallMap.get(id);
                    list.add(robBall);
                } else {
                    ActionGetBall getBall = getBallMap.get(id);
                    getBall.setTarget(target);
                    list.add(getBall);
                }
                break;
            }
            case RunningState.DRIBBLE: {
                ActionDribble dribble = dribbleMap.get(id);
                movePos = new Vector2D(canShootLine + 200, robotPos.getY() * 0.9);
                if (movePos.getX() < robotPos.getX())
                    movePos = penaltyCenter;
                dribble.setTarget(movePos);
                dribble.setAngle(MathHelper.direction(target, robotPos));
                this.visualizerTargets.add(movePos);
                dribble.setOffsetVel(200);
                list.add(dribble);
                break;
            }
            case RunningState.DEFENSE: {
                ActionMove move = moveMap.get(id);
                if (oppRobot.isEmpty() || (ballPos.getX() > goalFront.getX() * 0.5
                        && !this.isOppShoot)) {
                    movePos = new Vector2D(wf.getMinX() + ROBOT_RAD, 0);
                    // ボールが2つのゴールの間にある時
                    // clampのエラー防止のため
                } else if (FastMath.abs(ballPos.getX()) < goalFront.getX()) {
                    double oppShootAngle;
                    // 角度と位置の決定
                    if (isOppShoot) {
                        // シュートされたとき
                        oppShootAngle = MathHelper.direction(ballVel);
                        if (ballVel.getNorm() > 500)
                            defenseLine = ballPos.add(ballVel.scalarMultiply(2.0)).getX();
                        else
                            defenseLine = robotPos.getX();
                    } else {
                        // 相手がボールを持っているとき
                        oppShootAngle = oppRobot.get().getRobot().getTheta();
                        defenseLine = robotPos.getX();
                    }
                    // シュート方向がゴール外ならゴールの端へ
                    oppShootAngle = MathHelper.clamp(MathHelper.wrap2PI(oppShootAngle),
                            MathHelper.wrap2PI(MathHelper.direction(goalBack.add(goalWidth).subtract(ballPos))),
                            MathHelper.wrap2PI(MathHelper.direction(goalBack.subtract(goalWidth).subtract(ballPos))));
                    {// 相手に近づく条件
                        if (oppRobot.isPresent()) {
                            if (ballVel.getNorm() < 1000 && MathHelper.inferiorAngle(oppRobot.get().getRobot().getTheta(),
                                    MathHelper.direction(oppRobotPos.get().subtract(robotPos))) < FastMath.PI * 0.3) {
                                defenseLine = defenseLine + 1000 + ballVel.getX();
                            }
                            if (!isOppShoot && distBtoO.get() > 300) {
                                defenseLine += 1000 + ballVel.getX();
                            }
                        }
                    }
                    {// 相手から離れる条件
                        // ボールが自分に近いとき
                        if (ballPos.getX() - defenseLine < ROBOT_RAD + BALL_RAD)
                            defenseLine = ballPos.getX() - (ROBOT_RAD + BALL_RAD);
                        // 相手が自分に近いとき
                        if (oppRobotPos.get().getX() - defenseLine < ROBOT_RAD * 2 + BALL_RAD * 2)
                            defenseLine = oppRobotPos.get().getX() - ROBOT_RAD * 2 + BALL_RAD * 2 + 10;
                        // ペナルティエリアに近いとき
                        if (defenseLine > penaltyLine)
                            defenseLine = penaltyLine - ROBOT_RAD;
                        // ボールが一定以上の速度でゴール方向に向かっているとき
                        if (ballVel.getX() < -700) {
                            defenseLine += oppRobotVel.getX();
                        }
                    }
                    if (defenseLine < goalBack.getX()) {
                        defenseLine = goalBack.getX();
                    }
                    if (defenseLine > ballPos.getX() - ROBOT_RAD) {
                        defenseLine = ballPos.getX() - ROBOT_RAD;
                    }
                    movePos = ballPos.add(MathHelper.getFromPolar(ballPos.getX() - defenseLine,
                            MathHelper.wrapPI(oppShootAngle)));
                    visualizerTargets.add(movePos);
                } else {
                    movePos = goalBack.add(new Vector2D(ROBOT_RAD, 0));
                    if (ballPos.getY() > 0) {
                        movePos = movePos.add(goalWidth);
                    } else {
                        movePos = movePos.subtract(goalWidth);
                    }
                }
                visualizerTargets.add(movePos);
                if (ballVel.getX() < -800) {
                    ActionReceive receive = receiveMap.get(id);
                    list.add(receive);
                } else {
                    if (isOppShoot && ballPos.getX() < robotPos.getX()) {
                        // 自分よりボールのほうが自ゴールに近いとき
                        avoidBall = true;
                        move.setPos(goalBack.add(new Vector2D(0, ballPos.getY())));
                    } else {
                        move.setPos(movePos);
                    }
                    move.setAngle(MathHelper.direction(ballPos, robotPos));
                    list.add(move);
                }
                break;
            }
            case RunningState.SHOOT: {
                ActionKick kick = kickMap.get(id);
                kick.setTarget(target);
                kick.kickManually(new Pair<>(EnumKickType.STRAIGHT,
                        KickConverter.toPower(id, new Pair<>(EnumKickType.STRAIGHT, 3000.0))));
                list.add(kick);
                break;
            }
            case RunningState.STOPGAME: {
                ActionMove move = moveMap.get(id);
                move.setPos(new Vector2D(-500, 0));
                move.setAngle(0);
                list.add(move);
                break;
            }
            case RunningState.MITOMA: {
                ActionMitoma mitoma = mitomaMap.get(id);
                mitoma.setTarget(target);
                mitoma.kickManually(new Pair<>(EnumKickType.STRAIGHT,
                        KickConverter.toPower(id, new Pair<>(EnumKickType.STRAIGHT, 2000.0))));
                list.add(mitoma);
                break;
            }
        }
        List<AbstractAction> returnList = new ArrayList<>();
        for (AbstractAction action : list) {
            List<AbstractObstacle> obstacles = new ArrayList<>();
            obstacles.addAll(CommonObstacles.getFriendlyRobotsIdsExcluded(this.world.get(), this.color,
                    new int[]{action.getId()}, this.world.get().getFriendlyRobotMap(this.color).get(action.getId()).getRobot(), CommonObstacles.getMarginRobot()));
            obstacles.addAll(CommonObstacles.getOppositeRobots(this.world.get(), this.color,
                    this.world.get().getFriendlyRobotMap(this.color).get(action.getId()).getRobot(), CommonObstacles.getMarginRobot()));
            //obstacles.addAll(CommonObstacles.getFieldOutSideLinesDemo(this.world.get().getField(), CommonObstacles.getMarginRobot()));
            obstacles.addAll(Arrays.asList(new ObstacleSegment(goalBack.add(goalWidth), goalBack.add(goalWidth).subtract(goalLength), CommonObstacles.getMarginRobot() / 2),
                    new ObstacleSegment(goalBack.subtract(goalWidth), goalBack.subtract(goalWidth).subtract(goalLength), CommonObstacles.getMarginRobot() / 2)
            ));
            //obstacles.add(CommonObstacles.getOppositePenaltyArea(wf, CommonObstacles.getMarginRobot()));
            if (avoidBall) {
                obstacles.add(CommonObstacles.getObstacleBall(ballPos, ballVel, cycle, BALL_RAD + ROBOT_RAD));
            }
            obstacles.addAll(CommonObstacles.getOppositeGoal(wf, ROBOT_RAD));
            returnList.add(new WithPlanner<>(action, action.getId(), this.color, obstacles));
        }
        return returnList;
    }

    @Override
    public String getName() {
        return "demo";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
