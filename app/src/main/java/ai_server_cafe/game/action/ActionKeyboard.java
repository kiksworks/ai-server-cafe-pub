package ai_server_cafe.game.action;

import ai_server_cafe.device.keyboard.StatusKeyboard;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.UpdaterKeyboard;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ActionKeyboard extends AbstractAction {
    private final Logger logger = LogManager.getLogger("keyboard");
    private int dribblePowerBuffer;
    private int kickPowerBuffer;
    private StatusKeyboard state;
    private boolean lastPowerDown;
    private boolean lastPowerUp;

    public ActionKeyboard(int id, TeamColor color) {
        super(id, color);
        this.dribblePowerBuffer = 0;
        this.kickPowerBuffer = 150;
        this.lastPowerDown = false;
        this.lastPowerUp = false;
        this.state = StatusKeyboard.EMPTY;
    }

    public Command update(long now) {
        this.now = now;
        return this.execute();
    }

    @Override
    protected Command execute() {
        Command command = new Command().setDirect(true);
        command.setTargetVel(Vector2D.ZERO);
        command.setTargetOmega(0.0);
        if (this.world.isEmpty()) return command;

        if (this.state == StatusKeyboard.EMPTY) return command;
        double vx = 0.0;
        double vy = 0.0;
        double omega = 0.0;
        if (this.state.getStatus("forward")) vx += 400.0;
        if (this.state.getStatus("back")) vx -= 400.0;
        if (this.state.getStatus("forward") && this.state.getStatus("back")) vx = 0.0;
        if (this.state.getStatus("left")) vy += 400.0;
        if (this.state.getStatus("right")) vy -= 400.0;
        if (this.state.getStatus("left") && this.state.getStatus("right")) vy = 0.0;
        if (this.state.getStatus("leftRotate")) omega += 4.0;
        if (this.state.getStatus("rightRotate")) omega -= 4.0;
        if (this.state.getStatus("leftRotate") && this.state.getStatus("rightRotate")) omega = 0.0;
        Vector2D velocity = new Vector2D(vx, vy);
        if (UpdaterKeyboard.getInstance().isKeyPressed("ctrl")) {
            velocity = velocity.scalarMultiply(1.5);
            omega = 1.5 * omega;
        }
        command.setTargetVel(velocity);
        command.setTargetOmega(omega);

        if (!this.lastPowerDown && this.state.getStatus("powerDown")) {
            if (UpdaterKeyboard.getInstance().isKeyPressed("shift")) {
                this.kickPowerBuffer = Math.clamp(this.kickPowerBuffer - 10, 0, 250);
                this.logger.info("set kick power down : {}", this.kickPowerBuffer);
            } else {
                this.dribblePowerBuffer = Math.clamp(this.dribblePowerBuffer - 1, -3, 12);
                this.logger.info("set dribble power down : {}", this.dribblePowerBuffer);
            }
        }

        if (!this.lastPowerUp && this.state.getStatus("powerUp")) {
            if (UpdaterKeyboard.getInstance().isKeyPressed("shift")) {
                this.kickPowerBuffer = Math.clamp(this.kickPowerBuffer + 10, 0, 250);
                this.logger.info("set kick power up : {}", this.kickPowerBuffer);
            } else {
                this.dribblePowerBuffer = Math.clamp(this.dribblePowerBuffer + 1, -3, 12);
                this.logger.info("set dribble power up : {}", this.dribblePowerBuffer);
            }
        }

        if (this.state.getStatus("chip")) {
            command.setKickFlag(EnumKickType.CHIP, this.kickPowerBuffer);
            this.logger.info("chip kick with {}", this.kickPowerBuffer);
        } else if (this.state.getStatus("straight")) {
            command.setKickFlag(EnumKickType.STRAIGHT, this.kickPowerBuffer);
            this.logger.info("straight kick with {}", this.kickPowerBuffer);
        }
        if (this.state.getStatus("dribble")) {
            command.setDribble(this.dribblePowerBuffer);
        }

        this.lastPowerDown = this.state.getStatus("powerDown");
        this.lastPowerUp = this.state.getStatus("powerUp");

        return command;
    }

    @Override
    public String getName() {
        return "keyboard";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setKeyboardState(StatusKeyboard state) {
        this.state = state;
    }
}
