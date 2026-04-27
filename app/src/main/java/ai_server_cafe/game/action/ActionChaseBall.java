package ai_server_cafe.game.action;

import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.game.ControlHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import java.util.Optional;

public class ActionChaseBall extends AbstractAction {
    private Optional<Vector2D> hadBallPos;
    private Optional<Double> lastTargetTheta;
    private double lastTime;
    private boolean hasBall;
    private double hasBallCount;
    private boolean wraparound;
    private double margin;
    private Optional<Vector2D> predictedBallPos;
    private boolean autoWrap;

    public ActionChaseBall(int id, TeamColor color) {
        super(id, color);
        this.hadBallPos = Optional.empty();
        this.hasBall = true;
        this.hasBallCount = 0;
        this.wraparound = false;
        this.lastTargetTheta = Optional.empty();
        this.lastTime = 0.0;
        this.margin = ConfigManager.getInstance().getConfig().robotRadius;
        this.predictedBallPos = Optional.empty();
        this.autoWrap = false;
    }

    @Override
    protected Command execute() {
        Command command = new Command();
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id))
            return command;
        final World WORLD = this.world.get();
        final FilteredRobot robot = WORLD.getFriendlyRobotMap(this.color).get(this.id).getRobot();
        final double FACE_DIST = ConfigManager.getInstance().getConfig().toFaceRadius;
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;
        final double BALL_RAD = ConfigManager.getInstance().getConfig().ballRadius;
        final Vector2D robotPos = robot.position();
        Vector2D faceBallPos = robotPos.add(MathHelper.getFromPolar(FACE_DIST + BALL_RAD, robot.getTheta()));
        Vector2D ballPredictPos = WORLD.getBall().position();
        Vector2D ballVel = WORLD.getBall().velocity();
        if (WORLD.getBall().isLost() && this.hasBall) {
            ballPredictPos = faceBallPos;
        }
        this.hasBall = (faceBallPos.subtract(ballPredictPos)).getNorm() < 35.0 ||
                (MathHelper.inferiorAngle(robot.getTheta(),
                        MathHelper.direction(ballPredictPos, robotPos)) < 0.2 &&
                        MathHelper.distance2D(ballPredictPos, robotPos) < FACE_DIST + BALL_RAD);
        this.predictedBallPos = Optional.of(ballPredictPos);
        if (this.hasBall) {
            this.hasBallCount++;
        } else {
            this.hadBallPos = Optional.of(ballPredictPos);
            this.hasBallCount = 0;
        }

        if (this.autoWrap) {
            if (ballVel.getNorm() > 1000.0) {
                this.wraparound = true;
            }
        }

        final double tempInfAngle = MathHelper.inferiorAngle(robot.getTheta(), MathHelper.direction(ballPredictPos, robotPos));
        final double tempRobotRad = (tempInfAngle < MathHelper.HALF_PI) ? FastMath.min(FACE_DIST / FastMath.cos(tempInfAngle), ROBOT_RAD) : ROBOT_RAD;
        this.margin = tempRobotRad;
        double now = TimeHelper.now();
        double tempTargetTheta = (now - this.lastTime > 2.0 * ConfigManager.getInstance().getConfig().getCycleTime() || this.lastTargetTheta.isEmpty())
                ? MathHelper.direction(ballPredictPos, robotPos) : this.lastTargetTheta.get();
        double targetTheta = ballVel.getNorm() == 0.0 ? tempTargetTheta : MathHelper.direction(ballVel.scalarMultiply(this.wraparound && !this.hasBall ? -1.0 : 1.0));
        this.lastTime = now;
        command.setTargetTheta(targetTheta);
        Vector2D targetPos = ballPredictPos.add(MathHelper.getFromPolar(tempRobotRad + BALL_RAD, targetTheta).scalarMultiply(-1.0));
        Vector2D r = targetPos.subtract(robotPos);
        Vector2D targetVel = MathHelper.normalized(r).scalarMultiply(ControlHelper.getVelocityTrapezoidal(3000.0, r.getNorm(), 100.0));
        command.setTargetVel(targetVel.add(ballVel));
        command.setDribble(12);
        return command;
    }

    @Override
    public String getName() {
        return "chase_ball";
    }

    @Override
    public boolean isFinished() {
        return this.hasBallCount > 15;
    }

    public void setWraparound(boolean flag) {
        this.wraparound = flag;
    }

    public double getMargin() {
        return margin;
    }

    public Optional<Vector2D> getOptBallPos() {
        return this.predictedBallPos;
    }

    public void setAutoWrap(boolean flag) {
        this.autoWrap = flag;
    }
}
