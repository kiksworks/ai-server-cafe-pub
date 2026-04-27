package ai_server_cafe.game.action;

import ai_server_cafe.device.joystick.StatusJoyStick;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class ActionController extends AbstractAction {
    private int dribblePowerBuffer;
    private int kickPowerBuffer;
    private StatusJoyStick state;
    private final boolean[] lastPush;
    private final Logger logger;

    public ActionController(int id, TeamColor color) {
        super(id, color);
        this.dribblePowerBuffer = 0;
        this.kickPowerBuffer = 150;
        this.lastPush = new boolean[6];
        Arrays.fill(this.lastPush, false);
        this.logger = LogManager.getLogger("controller");
    }

    public Command update(long now) {
        this.now = now;
        return this.execute();
    }

    @Override
    protected Command execute() {
        Command command = new Command().setDirect(true);
        boolean x = this.state.buttonX;
        boolean y = this.state.buttonY;
        boolean a = this.state.buttonA;
        boolean b = this.state.buttonB;
        boolean l = this.state.leftButton;
        boolean r = this.state.rightButton;

        if (!this.lastPush[0] && x) {
            this.kickPowerBuffer = MathHelper.clamp(this.kickPowerBuffer - 10, 0, 250);
            this.logger.info("set kick power down : {}", this.kickPowerBuffer);
        }
        if (!this.lastPush[3] && b) {
            this.kickPowerBuffer = MathHelper.clamp(this.kickPowerBuffer + 10, 0, 250);
            this.logger.info("set kick power up : {}", this.kickPowerBuffer);
        }
        if (!this.lastPush[1] && y) {
            this.dribblePowerBuffer = MathHelper.clamp(this.dribblePowerBuffer + 1, -3, 12);
            this.logger.info("set dribble power up : {}", this.dribblePowerBuffer);
        }
        if (!this.lastPush[2] && a) {
            this.dribblePowerBuffer = MathHelper.clamp(this.dribblePowerBuffer - 1, -3, 12);
            this.logger.info("set dribble power down : {}", this.dribblePowerBuffer);
        }

        if (!this.lastPush[5] && r) {
            command.setKickFlag(EnumKickType.CHIP, this.kickPowerBuffer);
            this.logger.info("chip kick with {}", this.kickPowerBuffer);
        }

        if (!this.lastPush[4] && l) {
            command.setKickFlag(EnumKickType.STRAIGHT, this.kickPowerBuffer);
            this.logger.info("straight kick with {}", this.kickPowerBuffer);
        }

        int dribble = MathHelper.clamp((int) (this.state.leftTrigger + 1.0) * 4, 0, 12);
        command.setDribble(dribble == 0 ? this.dribblePowerBuffer : dribble);
        command.setTargetVel(new Vector2D(-1500.0 * this.state.leftY, -1500.0 * this.state.leftX));
        command.setTargetOmega(-6.0 * FastMath.asin(this.state.rightX) * 2.0 / FastMath.PI);

        this.lastPush[0] = x;
        this.lastPush[1] = y;
        this.lastPush[2] = a;
        this.lastPush[3] = b;
        this.lastPush[4] = l;
        this.lastPush[5] = r;
        return command;
    }

    @Override
    public String getName() {
        return "control";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setControllerStatus(StatusJoyStick state) {
        this.state = state;
    }
}
