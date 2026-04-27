package ai_server_cafe.game.action;

import ai_server_cafe.model.game.Command;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.Optional;

public class ActionTestMove extends AbstractAction {
    private Optional<Vector2D> optPos;
    private Optional<Vector2D> optVel;
    private Optional<Double> optTheta;
    private Optional<Double> optOmega;
    private Vector2D lastTargetPos;
    private double lastStartedTime;
    private boolean alreadyGoaled;
    private int count;
    private final int holdCount;
    private double prevMeasuredTime;

    public ActionTestMove(int id, TeamColor color) {
        super(id, color);
        this.optPos = Optional.empty();
        this.optVel = Optional.empty();
        this.optTheta = Optional.empty();
        this.optOmega = Optional.empty();
        this.lastTargetPos = Vector2D.ZERO;
        this.lastStartedTime = 0.0;
        this.alreadyGoaled = true;
        this.count = 0;
        this.holdCount = 60;
        this.prevMeasuredTime = 0.0;
    }

    @Override
    protected Command execute() {
        Command command = new Command();
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id))
            return command;

        if(this.optVel.isPresent()) {
            command.setTargetVel(this.optVel.get());
            optVel = Optional.empty();
        } else if (this.optPos.isPresent()) {
            command.setTargetPosition(this.optPos.get());
            if (!this.optPos.get().equals(this.lastTargetPos)) {
                this.lastStartedTime = TimeHelper.now();
                this.lastTargetPos = this.optPos.get();
                this.alreadyGoaled = false;
                this.count = 0;
            }
            if (this.world.get().getFriendlyRobotMap(this.color).get(this.id).getRobot().position().subtract(this.lastTargetPos).getNorm() < 200.0 && this.count <= this.holdCount) {
                this.count++;
            } else {
                this.count = 0;
            }
            if (this.world.get().getFriendlyRobotMap(this.color).get(this.id).getRobot().position().subtract(this.lastTargetPos).getNorm() < 200.0 && !this.alreadyGoaled) {
                double d = TimeHelper.now() - this.lastStartedTime;
                this.alreadyGoaled = true;
                if (d > 0.001) {
                    this.prevMeasuredTime = d;
                }
            }
        }
        if (this.optTheta.isPresent()) {
            command.setTargetTheta(this.optTheta.get());
        } else if (this.optOmega.isPresent()) {
            command.setTargetTheta(this.optOmega.get());
        }
        return command;
    }

    public void setPos(Vector2D pos) {
        this.optPos = Optional.of(pos);
    }

    public void setVel(Vector2D vel) {
        this.optVel = Optional.of(vel);
    }

    public void setAngle(double theta) {
        this.optTheta = Optional.of(theta);
    }

    public void setVelAngular(double omega) {
        this.optOmega = Optional.of(omega);
    }

    @Override
    public String getName() {
        return "move";
    }

    @Override
    public boolean isFinished() {
        return this.count > this.holdCount;
    }

    public double getPrevMeasuredTime() {
        return this.prevMeasuredTime;
    }
}
