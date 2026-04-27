package ai_server_cafe.game.action;

import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.util.TeamColor;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import java.util.Optional;

public class ActionMove extends AbstractAction {
    private Optional<Vector2D> optPos;
    private Optional<Vector2D> optVel;
    private Optional<Double> optTheta;
    private Optional<Double> optOmega;

    public ActionMove(int id, TeamColor color) {
        super(id, color);
        this.optPos = Optional.empty();
        this.optVel = Optional.empty();
        this.optTheta = Optional.empty();
        this.optOmega = Optional.empty();
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
        }
        if (this.optOmega.isPresent()) {
            command.setTargetOmega(this.optOmega.get());
            this.optOmega = Optional.empty();
        } else if (this.optTheta.isPresent()) {
            command.setTargetTheta(this.optTheta.get());
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
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id) || true)
            return false;
        IntegratedRobot iRobot = this.world.get().getFriendlyRobotMap(this.color).get(this.id);
        return (this.optPos.isEmpty() || this.optPos.get().subtract(iRobot.getRobot().position()).getNorm() < 5.0)
                && (this.optTheta.isEmpty() || FastMath.abs(this.optTheta.get() - iRobot.getRobot().getTheta()) < 0.1);
    }
}
