package ai_server_cafe.game.stats;

import ai_server_cafe.game.strategy.XGType;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterStats;
import ai_server_cafe.updater.UpdaterStrategy;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;

public class StatsMaker {
    private static final Logger LOGGER = LogManager.getLogger("shoot_XG");
    // ボールの状態の表現
    public enum BallState {
        FREE,
        BLUE_HAVE_BALL,
        YELLOW_HAVE_BALL,
        STACK,
        BLUE_KICKED,
        YELLOW_KICKED
    }

    // ボールの状態
    private  BallState ballState;

    // 最後にボールに触れたロボット
    private int lastBlueId;
    private int lastYellowId;

    public StatsMaker() {
        this.ballState = BallState.FREE;
        this.lastBlueId = 0;
        this.lastYellowId = 0;
    }

    public void update() {
        final World world = UpdaterWorld.getInstance().getWorld(false);
        final Field wf = world.getField();

        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;

        final Vector2D ballPos = world.getBall().position();
        final Vector2D ballVel = world.getBall().velocity();
        Map<Integer, IntegratedRobot> blueRobots = world.getFriendlyRobotMap(TeamColor.BLUE);
        Map<Integer, IntegratedRobot> yellowRobots = world.getFriendlyRobotMap(TeamColor.YELLOW);

        // ボールに一番近いロボット
        Optional<IntegratedRobot> blueNearest = MathHelper.nearestRobotToPosition(blueRobots, ballPos);
        Optional<IntegratedRobot> yellowNearest = MathHelper.nearestRobotToPosition(yellowRobots, ballPos);

        int blueNearestId =0;
        boolean blueNearestIdVisible =false;
        if (blueNearest.isPresent()) {
            blueNearestId = (int)blueNearest.get().getId();
            blueNearestIdVisible = true;
        } else {
            //LOGGER.info("blueのボールホルダが見つかりません");
        }

        int yellowNearestId =0;
        boolean yellowNearestIdVisible =false;
        if (yellowNearest.isPresent()) {
            yellowNearestId = (int)yellowNearest.get().getId();
            yellowNearestIdVisible = true;
        } else {
            //LOGGER.info("yellowのボールホルダが見つかりません");
        }

        boolean blueHaveBall = blueNearest.isPresent()
                && MathHelper.distancePositionToRobot(ballPos, blueNearest.get().getRobot()) < ROBOT_RAD * 2;
        boolean yellowHaveBall = yellowNearest.isPresent()
                && MathHelper.distancePositionToRobot(ballPos, yellowNearest.get().getRobot()) < ROBOT_RAD * 2;

        // オーバードリブル対策用
        UpdaterWorld.getInstance().setHaveBall(blueHaveBall, TeamColor.BLUE);
        UpdaterWorld.getInstance().setHaveBall(yellowHaveBall, TeamColor.YELLOW);

        UpdaterStats stats = UpdaterStats.getInstance();
        UpdaterStrategy xG = UpdaterStrategy.getInstance();

        BallState lastState = this.ballState;

        // 状態遷移
        if (!blueHaveBall && !yellowHaveBall && ballVel.getNorm() < 200) {
            this.ballState = BallState.FREE;
        }
        switch (ballState) {
            case BallState.FREE:
                if (blueHaveBall && yellowHaveBall) {
                    this.ballState = BallState.STACK;
                } else if (blueHaveBall) {
                    this.ballState = BallState.BLUE_HAVE_BALL;
                } else if (yellowHaveBall) {
                    this.ballState = BallState.YELLOW_HAVE_BALL;
                }
                break;

            case BallState.BLUE_HAVE_BALL:
                if (!blueHaveBall && !yellowHaveBall) {
                    this.ballState = BallState.BLUE_KICKED;
                } else if (blueHaveBall && yellowHaveBall) {
                    this.ballState = BallState.STACK;
                } else if (yellowHaveBall) {
                    this.ballState = BallState.YELLOW_HAVE_BALL;
                }
                break;

            case BallState.YELLOW_HAVE_BALL:
                if (!blueHaveBall && !yellowHaveBall) {
                    this.ballState = BallState.YELLOW_KICKED;
                } else if (blueHaveBall && yellowHaveBall) {
                    this.ballState = BallState.STACK;
                } else if (blueHaveBall) {
                    this.ballState = BallState.BLUE_HAVE_BALL;
                }
                break;

            case BallState.BLUE_KICKED:
                if (blueHaveBall) {
                    this.ballState = BallState.BLUE_HAVE_BALL;
                } else if (yellowHaveBall) {
                    this.ballState = BallState.YELLOW_HAVE_BALL;
                }
                break;

            case BallState.YELLOW_KICKED:
                if (yellowHaveBall) {
                    this.ballState = BallState.YELLOW_HAVE_BALL;
                } else if (blueHaveBall) {
                    this.ballState = BallState.BLUE_HAVE_BALL;
                }
                break;

            case BallState.STACK:
                if (blueHaveBall && !yellowHaveBall) {
                    this.ballState = BallState.BLUE_HAVE_BALL;
                } else if (yellowHaveBall && !blueHaveBall) {
                    this.ballState = BallState.YELLOW_HAVE_BALL;
                } else if (!blueHaveBall && !yellowHaveBall) {
                    if (ballVel.getNorm() > 500 && blueNearest.isPresent() && yellowNearest.isPresent()) {
                        // どちらがキックしたかわからないとき
                        // ボールから遠いロボットがキックしたとする
                        Vector2D bluePos = blueNearest.get().getRobot().position();
                        Vector2D yellowPos = yellowNearest.get().getRobot().position();
                        if (MathHelper.distance2D(bluePos, ballPos) > MathHelper.distance2D(yellowPos, ballPos)) {
                            this.ballState = BallState.BLUE_KICKED;
                        } else {
                            this.ballState = BallState.YELLOW_KICKED;
                        }
                    } else {
                        this.ballState = BallState.FREE;
                    }
                }
                break;
        }

        // 状態の切り替わりでパス、キック、シュートなどをカウント
        switch (ballState) {
            case BallState.FREE:
                break;

            case BallState.BLUE_HAVE_BALL:
                if (lastState == BallState.BLUE_KICKED
                        && this.lastBlueId != blueNearest.get().getId()) {
                    // パスをカウント(Blue)
                    stats.setPassCount(stats.getPassCount(TeamColor.BLUE) + 1, TeamColor.BLUE);
                } else if (lastState == BallState.YELLOW_KICKED) {
                    // YellowがキックしたボールをBlueが取ったとき
                    // パスカットをカウント(Blue)
                    stats.setPassCutCount(stats.getPassCutCount(TeamColor.BLUE) + 1, TeamColor.BLUE);
                }

                if (lastState != BallState.BLUE_HAVE_BALL) {
                    // 最後にボールに触れたロボット
                    this.lastBlueId = blueNearest.get().getId();
                }
                break;

            case BallState.YELLOW_HAVE_BALL:
                if (lastState == BallState.YELLOW_KICKED
                        && this.lastYellowId != yellowNearest.get().getId()) {
                    // パスをカウント(Yellow)
                    stats.setPassCount(stats.getPassCount(TeamColor.YELLOW) + 1, TeamColor.YELLOW);
                } else if (lastState == BallState.BLUE_KICKED) {
                    // BlueがキックしたボールをYellowが取ったとき
                    // パスカットをカウント(Yellow)
                    stats.setPassCutCount(stats.getPassCutCount(TeamColor.YELLOW) + 1, TeamColor.YELLOW);
                }

                if (lastState != BallState.YELLOW_HAVE_BALL) {
                    // 最後にボールに触れたロボット
                    this.lastYellowId = yellowNearest.get().getId();
                }
                break;

            case BallState.BLUE_KICKED:
                if (lastState != BallState.BLUE_KICKED) {
                    // キックをカウント(Blue)
                    stats.setKickCount(stats.getKickCount(TeamColor.BLUE) + 1, TeamColor.BLUE);
                    if (isShoot(TeamColor.BLUE)) {
                        // シュートをカウント
                        stats.setShootCout(stats.getShootCount(TeamColor.BLUE) + 1, TeamColor.BLUE);
                        if (isShootInFrame(TeamColor.BLUE)) {
                            // 枠内シュートをカウント
                            stats.setShootInFrameCout(stats.getShootInFrameCount(TeamColor.BLUE) + 1, TeamColor.BLUE);
                        }
                        if(blueNearestIdVisible) {
                            //　ゴール期待値を計算＆記録
                            stats.setShootInFrameXG(stats.getShootInFrameXG(TeamColor.BLUE) + xG.getXG(TeamColor.BLUE, blueNearestId, XGType.TYPEC), TeamColor.BLUE);
                            String result = "blue " + blueNearestId + " XG="+xG.getXG(TeamColor.BLUE, blueNearestId,XGType.TYPEC);
                            LOGGER.info(result);
                        }
                    }
                }
                break;

            case BallState.YELLOW_KICKED:
                if (lastState != BallState.YELLOW_KICKED) {
                    // キックをカウント(Yellow)
                    stats.setKickCount(stats.getKickCount(TeamColor.YELLOW) + 1, TeamColor.YELLOW);
                    if (isShoot(TeamColor.YELLOW)) {
                        // シュートをカウント
                        stats.setShootCout(stats.getShootCount(TeamColor.YELLOW) + 1, TeamColor.YELLOW);
                        if (isShootInFrame(TeamColor.YELLOW)) {
                            // 枠内シュートをカウント
                            stats.setShootInFrameCout(stats.getShootInFrameCount(TeamColor.YELLOW) + 1, TeamColor.YELLOW);
                        }
                        if(yellowNearestIdVisible) {
                            //　ゴール期待値を計算＆記録
                            stats.setShootInFrameXG(stats.getShootInFrameXG(TeamColor.YELLOW) + xG.getXG(TeamColor.YELLOW, yellowNearestId,XGType.TYPEC), TeamColor.YELLOW);
                            String result = "yellow " + yellowNearestId + " XG="+xG.getXG(TeamColor.YELLOW, yellowNearestId,XGType.TYPEC);
                            LOGGER.info(result);
                        }
                    }
                }
                break;

            case BallState.STACK:
                break;
        }

        // どっちがボールを支配しているか
        if (ballState == BallState.BLUE_HAVE_BALL || ballState == BallState.BLUE_KICKED) {
            stats.setBallPossessionCount(1 + stats.getBallPossessionCount(TeamColor.BLUE), TeamColor.BLUE);
        } else if (ballState == BallState.YELLOW_HAVE_BALL || ballState == BallState.YELLOW_KICKED) {
            stats.setBallPossessionCount(1 + stats.getBallPossessionCount(TeamColor.YELLOW), TeamColor.YELLOW);
        } else if (ballState == BallState.STACK) {
            stats.setBallPossessionCount(1 + stats.getBallPossessionCount(TeamColor.BLUE), TeamColor.BLUE);
            stats.setBallPossessionCount(1 + stats.getBallPossessionCount(TeamColor.YELLOW), TeamColor.YELLOW);
        }
    }

    // シュートかの判定を行う
    private boolean isShoot(TeamColor color) {
        final World world = UpdaterWorld.getInstance().getWorld(ConfigManager.getInstance().isInvert(color));
        final Field wf = world.getField();
        final Vector2D ballPos = world.getBall().position();
        final Vector2D ballVel = world.getBall().velocity();

        // ペナルティーエリアの角
        Vector2D frontGoalMax = new Vector2D(wf.getMaxX(), wf.getPenaltyMaxY());
        Vector2D frontGoalMin = new Vector2D(wf.getMaxX(), wf.getPenaltyMinY());

        return ballVel.getNorm() > 1500
                && MathHelper.distance2D(ballPos, wf.getFrontGoalCenter()) < wf.getMaxY()
                && MathHelper.isRightOf(MathHelper.direction(ballVel), MathHelper.direction(frontGoalMax, ballPos))
                && MathHelper.isLeftOf(MathHelper.direction(ballVel), MathHelper.direction(frontGoalMin, ballPos));
    }

    // 枠内シュートかの判定を行う
    private boolean isShootInFrame(TeamColor color) {
        final World world = UpdaterWorld.getInstance().getWorld(ConfigManager.getInstance().isInvert(color));
        final Field wf = world.getField();
        final Vector2D ballPos = world.getBall().position();
        final Vector2D ballVel = world.getBall().velocity();

        // ペナルティーエリアの角
        Vector2D frontGoalMax = new Vector2D(wf.getMaxX(), wf.getGoalMaxY());
        Vector2D frontGoalMin = new Vector2D(wf.getMaxX(), wf.getGoalMinY());

        return ballVel.getNorm() > 1500
                && MathHelper.distance2D(ballPos, wf.getFrontGoalCenter()) < wf.getMaxY()
                && MathHelper.isRightOf(MathHelper.direction(ballVel), MathHelper.direction(frontGoalMax, ballPos))
                && MathHelper.isLeftOf(MathHelper.direction(ballVel), MathHelper.direction(frontGoalMin, ballPos));
    }
}
