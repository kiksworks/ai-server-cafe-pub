package ai_server_cafe.scoreboard.demo_autoref;
// 得点のみに機能を絞ったDemo用簡易Autoref

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.model.game.EnumCaptainType;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterScoreBoard;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;
import kotlin.Pair;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;

enum GameState {
    IN_FIELD, READY_BLUE_GOAL, READY_YEL_GOAL, OUT_FIELD
}

public final class DemoAutoRef extends AbstractLoopThreadCafe {
    private static DemoAutoRef instance = null;

    private final int GoalDecisionCount;  // readyに移行した後ゴールとするフレーム数
    private final int VelocityHistorySize; // Goal方向に進んでるか判定するフレーム数
    private final double GoalCoolTime; // Goal後に次にGoal準備に入る時間

    private GameState state;
    private int GoalCount;
    private double lastGoalTime;
    private double lastLoopTime;
    ArrayList<Pair<Double, Double>> ballVelocity;

    private DemoAutoRef() {
        super("demo_autoref");
        state = GameState.IN_FIELD;
        GoalCount = 0;
        ballVelocity = new ArrayList<>();
        GoalDecisionCount = ConfigManager.getInstance().getConfig().demoAutorefConfig.goalDecisionCount;
        VelocityHistorySize = ConfigManager.getInstance().getConfig().demoAutorefConfig.velocityHistorySize;
        GoalCoolTime = ConfigManager.getInstance().getConfig().demoAutorefConfig.goalCoolTIme;
    }

    public static DemoAutoRef getInstance() {
        if (instance == null) {
            instance = new DemoAutoRef();
        }
        return instance;
    }

    @Override
    protected void loop() {
        ConfigManager configManager = ConfigManager.getInstance();
        UpdaterWorld updater = UpdaterWorld.getInstance();

        if (TimeHelper.now() - lastLoopTime > configManager.getConfig().getCycleTime()) {

            if (configManager.isStart()){

                this.lastLoopTime = TimeHelper.now();
                FilteredBall ball = updater.getWorld(false).getBall();

                ballVelocity.add(new Pair<>(ball.getVx(), ball.getVy()));
                if (ballVelocity.size() > VelocityHistorySize) {
                    ballVelocity.removeFirst();
                }

                switch (state) {
                    case IN_FIELD:
                        GoalCount = 0;
                        if (isBallEnteringGoal(TeamColor.YELLOW)) {
                            state = GameState.READY_YEL_GOAL;
                        } else if (isBallEnteringGoal(TeamColor.BLUE)) {
                            state = GameState.READY_BLUE_GOAL;
                        } else if (!isBallInField()) {
                            state = GameState.OUT_FIELD;
                        }
                        break;

                    case READY_YEL_GOAL:
                        logger.debug("Ready Yellow Goal : {}", GoalCount);
                        if (isBallInGoal(TeamColor.YELLOW)) {
                            GoalCount++;
                        } else if (isBallInField()) {
                            state = GameState.IN_FIELD;
                        } else {
                            state = GameState.OUT_FIELD;
                        }
                        // Goal
                        if (GoalCount >= GoalDecisionCount) {
                            UpdaterScoreBoard.getInstance().goalYellow(isCaptainAi(TeamColor.YELLOW));
                            notifyUpdate();

                            // AIの強さを調整
                            this.updateAiLevel(UpdaterScoreBoard.getInstance().getScoreBlue(), UpdaterScoreBoard.getInstance().getScoreYellow());

                            logger.info("Yellow Goal :  {} - {}", UpdaterScoreBoard.getInstance().getScoreYellow(), UpdaterScoreBoard.getInstance().getScoreBlue());
                            GoalCount = 0;
                            lastGoalTime = TimeHelper.now();
                            state = GameState.OUT_FIELD;
                        }
                        break;

                    case READY_BLUE_GOAL:
                        logger.debug("Ready Blue Goal : {}", GoalCount);
                        // 状態移動
                        if (isBallInGoal(TeamColor.BLUE)) {
                            GoalCount++;
                        } else if (isBallInField()) {
                            state = GameState.IN_FIELD;
                        } else {
                            state = GameState.OUT_FIELD;
                        }
                        // Goal
                        if (GoalCount >= GoalDecisionCount) {
                            UpdaterScoreBoard.getInstance().goalBlue(isCaptainAi(TeamColor.BLUE));
                            notifyUpdate();

                            // AIの強さを調整
                            this.updateAiLevel(UpdaterScoreBoard.getInstance().getScoreBlue(), UpdaterScoreBoard.getInstance().getScoreYellow());

                            logger.info("Blue Goal :  {} - {}", UpdaterScoreBoard.getInstance().getScoreYellow(), UpdaterScoreBoard.getInstance().getScoreBlue());
                            lastGoalTime = TimeHelper.now();
                            GoalCount = 0;
                            state = GameState.OUT_FIELD;
                        }
                        break;

                    case OUT_FIELD:
                        GoalCount = 0;
                        if (isBallInField() && TimeHelper.now() - lastGoalTime > GoalCoolTime) {
                            state = GameState.IN_FIELD;
                        }
                        break;
                }

            } else {
                this.state = GameState.IN_FIELD;
                this.ballVelocity.clear();
                this.GoalCount = 0;
            }
        }
    }

    @Override
    protected void init() {
        lastGoalTime = TimeHelper.now();
        lastLoopTime = TimeHelper.now();
    }

    private boolean isCaptainAi(TeamColor color) {
        EnumCaptainType captainType = ConfigManager.getInstance().getConfig().getCaptain(color);

        if (captainType == EnumCaptainType.CONTROLLER || captainType == EnumCaptainType.DEMOCONTROLLER) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isBallInField() {
        FilteredBall ball = UpdaterWorld.getInstance().getWorld(false).getBall();
        return FastMath.abs(ball.getX()) < UpdaterWorld.getInstance().getField().getMaxX()
                && FastMath.abs(ball.getY()) < UpdaterWorld.getInstance().getField().getMaxY();
    }

    private boolean isBallInGoal(TeamColor color) {
        Field field = UpdaterWorld.getInstance().getField();
        FilteredBall ball = UpdaterWorld.getInstance().getWorld(false).getBall();
        boolean isGoalNegative = ConfigManager.getInstance().isInvert(color);
        boolean isBallInGoal = (FastMath.abs(ball.getX()) > field.getMaxX()
                && FastMath.abs(ball.getX()) < (field.getMaxX() + field.getGoalLength())
                &&  FastMath.abs(ball.getY()) < field.getGoalMaxY());
        if (isBallInGoal) {
            return isGoalNegative ? ball.getX() < 0.0 : ball.getX() > 0.0;
        } else {
            return false;
        }
    }

    // ballがゴールに正しく侵入したか
    private boolean isBallEnteringGoal(TeamColor color){
        if (isBallInGoal(color)){
            boolean isGoalNegative = ConfigManager.getInstance().isInvert(color);
            //　ゴール方向(X成分のみ)
            double dirToGoal = isGoalNegative ? -1 : 1;

            for (Pair<Double, Double> vel : ballVelocity) {
                double vx = vel.getFirst();
                if (vx * dirToGoal <= 10) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private void notifyUpdate(){
        if (UpdaterScoreBoard.getInstance().isDirty()) {
            for (UpdaterScoreBoard.ScoreUpdateListener listener : UpdaterScoreBoard.getInstance().getListeners().get()) {
                listener.onScoreUpdate();
            }
            UpdaterScoreBoard.getInstance().resetDirty();
        }
    }

    // AIの強さを更新する
    private void updateAiLevel(int blueScore, int yellowScore) {
        if (blueScore >= yellowScore + 1) {
            UpdaterScoreBoard.getInstance().setAiLevel(TeamColor.BLUE, 0);
            UpdaterScoreBoard.getInstance().setAiLevel(TeamColor.YELLOW, 2);
        } else if (yellowScore >= blueScore + 1) {
            UpdaterScoreBoard.getInstance().setAiLevel(TeamColor.BLUE, 2);
            UpdaterScoreBoard.getInstance().setAiLevel(TeamColor.YELLOW, 0);
        } else if (yellowScore == blueScore) {
            UpdaterScoreBoard.getInstance().setAiLevel(TeamColor.BLUE, 1);
            UpdaterScoreBoard.getInstance().setAiLevel(TeamColor.YELLOW, 1);
        }
    }
}