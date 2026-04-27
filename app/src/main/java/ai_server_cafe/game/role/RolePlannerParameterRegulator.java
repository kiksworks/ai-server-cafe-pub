package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionTestMove;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.base.ObstacleCircle;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.updater.DynamicsManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RolePlannerParameterRegulator extends AbstractRole {
    private static final Logger LOGGER = LogManager.getLogger("pp-regulator");
    private static final double INIT_VALUE = 0.1;
    // 学習率 learningRate
    private static final double ETA = 0.8;
    private static final double ALPHA = 1.0;
    private static final int REPEAT_COUNT = 20;
    private final List<Vector2D> waitingPoses;
    protected Map<Integer, ActionTestMove> testMoveMap;
    private int currentIndex;
    private boolean isAllFinished;
    private double x;
    private double maxY;
    private int count;
    private boolean currentDirectionForward;
    private double prevForwardTime;
    private double prevBackTime;
    private boolean isInit;
    private double prevValue;

    public RolePlannerParameterRegulator(TeamColor color, int[] ids) {
        super(color, ids);
        this.testMoveMap = new HashMap<>();
        for (int id : ids) {
            this.testMoveMap.put(id, new ActionTestMove(id, color));
        }
        this.x = -6000;
        this.maxY = 4500;
        this.currentIndex = 0;
        this.count = 0;
        this.currentDirectionForward = true;
        this.prevForwardTime = 0;
        this.prevBackTime = 0;
        this.isInit = true;
        this.isAllFinished = false;
        this.waitingPoses = new ArrayList<>();
        this.prevValue = INIT_VALUE;
        for (int id = 0; id < 16; id++) {
            this.waitingPoses.add(new Vector2D(-500, (id - 8) * 300 + 150));
        }
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty()) {
            return list;
        }
        List<Integer> visible = getVisibleIds(this.world.get(), this.color, MathHelper.toArray(this.roleIds));
        if (visible.isEmpty()) {
            return list;
        }
        boolean isTypeB = !WithPlanner.USE_OLD_PLANNER;
        Vector2D targetForward = new Vector2D(x, maxY);
        Vector2D targetBack = new Vector2D(x, -maxY);
        int currentId;
        if (visible.size() > this.currentIndex) {
            currentId = visible.get(this.currentIndex);
            ActionTestMove testMove = this.testMoveMap.get(currentId);
            if (this.isInit) {
                testMove.setPos(targetBack);
            } else {
                if (this.currentDirectionForward) {
                    testMove.setPos(targetForward);
                    testMove.setAngle(0.0);
                } else {
                    testMove.setPos(targetBack);
                    testMove.setAngle(MathHelper.PI);
                }
            }
            // switch when finished
            if (testMove.isFinished()) {
                double time = testMove.getPrevMeasuredTime();
                if (this.isInit) {
                    this.isInit = false;
                } else {
                    double alpha = ALPHA;
                    double error;
                    if (this.currentDirectionForward) {
                        error = this.prevForwardTime - time;
                        this.prevForwardTime = time;
                    } else {
                        error = this.prevBackTime - time;
                        this.prevBackTime = time;
                    }
                    double currentValue = DynamicsManager.getInstance().getCentripetalRatio(currentId, isTypeB);
                    double tempValue = MathHelper.sigmoidInverse(alpha, currentValue);
                    double prevTempValue = MathHelper.sigmoidInverse(alpha, this.prevValue);
                    double nextTempValue = tempValue +
                            ETA * (1.0 - (double) this.count / (REPEAT_COUNT)) * error *
                                    FastMath.copySign(1.0, tempValue - prevTempValue);
                    double nextValue = MathHelper.clamp(MathHelper.sigmoid(alpha, nextTempValue), 0.01, 0.99);
                    LOGGER.trace("{}:{} {} -> {}", this.color, currentId, currentValue, nextValue);
                    DynamicsManager.getInstance().setData(currentId, isTypeB, nextValue);
                    this.prevValue = currentValue;
                    this.currentDirectionForward = !this.currentDirectionForward;
                    this.count++;
                }
                if (this.count >= REPEAT_COUNT) {
                    this.currentIndex++;
                    this.isInit = true;
                    this.prevBackTime = 0;
                    this.prevForwardTime = 0;
                    this.prevValue = INIT_VALUE;
                    this.count = 0;
                }
            }
        } else {
            this.isAllFinished = true;
            currentId = -1;
        }
        for (int id : visible) {
            List<AbstractObstacle> obstacles = CommonObstacles.getAllExcludedARobot(this.world.get(), this.color, id,
                    CommonObstacles.getMarginRobot(),
                    this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot());
            Field field = this.world.get().getField();
            obstacles.add(new ObstacleCircle(
                    new Vector2D(field.getMinX() + 0.5 * field.getPenaltyLength(), field.getPenaltyMaxY()), 500.0,
                    CommonObstacles.getMarginRobot()));
            obstacles.add(new ObstacleCircle(
                    new Vector2D(field.getMinX() + 0.5 * field.getPenaltyLength(), -field.getPenaltyMaxY()), 500.0,
                    CommonObstacles.getMarginRobot()));
            if (currentId == id) {
                list.add(new WithPlanner<>(this.testMoveMap.get(id), id, this.color, obstacles, true));
            } else {
                this.testMoveMap.get(id).setAngle(0.0);
                this.testMoveMap.get(id).setPos(this.waitingPoses.get(id));
                list.add(new WithPlanner<>(this.testMoveMap.get(id), id, this.color, obstacles, true));
            }
        }
        return list;
    }

    @Override
    public String getName() {
        return "rolePlannerParameterRegulator";
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.maxY = FastMath.max(y, 0.0);
    }

    @Override
    public boolean isFinished() {
        return this.isAllFinished;
    }
}
