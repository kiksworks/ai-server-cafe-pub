package ai_server_cafe.game.action;

import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ActionCircularMove extends AbstractAction {
    private double targetR = 0.0;
    private double dTheta = 0.0;
    private Vector2D lastPos = Vector2D.ZERO;
    private Vector2D prevTargetVel = Vector2D.ZERO;
    private double ratio = 0.0;
    private double integratedError = 0.0;
    private double prevError = 0.0;
    private double lastRatio = 0.0;
    private double resultRatio = 0.0;
    public static final Logger logger = LogManager.getLogger("action-circular-move");

    public ActionCircularMove(int id, TeamColor color) {
        super(id, color);
    }

    @Override
    protected Command execute() {
        Command command = new Command();
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id))
            return command;
        double dt = 1.0 / 60.0;
        IntegratedRobot robot = this.world.get().getFriendlyRobotMap(this.color).get(this.id);
        Vector2D pos = robot.getRobot().position();
        double theta = MathHelper.direction(pos);
        double r = pos.getNorm();
        this.dTheta = MathHelper.clamp(this.dTheta + 0.000001 * (this.targetR - r), 0.1 * dt, 3.0 * dt);
        double error = this.targetR - r;
        double kp = 0.01;
        double ki = 0.0001;
        double kd = 0.0000000000;
        this.ratio = MathHelper.clamp(this.ratio + kp * error + ki * this.integratedError + kd * (error - this.prevError), 0.4, 1.0);
        double d = 0.1;
        //ratio = d * ratio + this.lastRatio * (1 - d);
        double vr = FastMath.sqrt(3500.0 * this.targetR);//r * this.dTheta / dt;
        Vector2D velVec = MathHelper.normalized(MathHelper.applyRotation2D(pos, MathHelper.HALF_PI));
        Vector2D velVecR = MathHelper.normalized(pos).negate();
        this.ratio = this.lastRatio * (1.0 - d) + d * this.ratio;
        double velH = vr * this.ratio;
        Vector2D targetVel = velVec.scalarMultiply(velH).add(velVecR.scalarMultiply(FastMath.sqrt(vr * vr - velH * velH)));
        double angle = theta - MathHelper.HALF_PI;
        this.lastPos = pos;
        command.setTargetVel(targetVel);
        command.setTargetTheta(angle);
        double d1 = 0.01;
        this.resultRatio = d1 * this.ratio + (1 - d1) * this.resultRatio;
        logger.info("Result Ratio : {}", this.resultRatio);
        this.prevTargetVel = targetVel;
        this.prevError = error;
        this.integratedError += error;
        this.lastRatio = ratio;
        return command;
    }

    public void setRadius(double r) {
        this.targetR = r;
    }

    @Override
    public String getName() {
        return "circleMove";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
