package ai_server_cafe.game.action;

import ai_server_cafe.device.joystick.StatusJoyStick;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import java.util.Arrays;

public class ActionDemoController extends AbstractAction {
    private int dribblePowerBuffer;
    private int kickPowerBuffer;
    private double targetTheta;
    private double velocityMax;
    private StatusJoyStick state;
    private final boolean[] lastPush;
    // キックパワー（ストレート、チップ）
    private Pair<Integer, Integer> kickPow;

    public ActionDemoController(int id, TeamColor color) {
        super(id, color);
        this.dribblePowerBuffer = 0;
        this.kickPowerBuffer = 150;
        this.targetTheta = 0;
        this.velocityMax = 2000;
        this.lastPush = new boolean[2];
        this.kickPow = new Pair<>(100, 100);
        Arrays.fill(this.lastPush, false);
    }

    public Command update() {
        return this.execute();
    }

    @Override
    protected Command execute() {
        Command command = new Command();
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id))
            return command;

        boolean l = this.state.leftButton;
        boolean r = this.state.rightButton;

        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;

        final Vector2D ballPos = world.get().getBall().position();

        final FilteredRobot robot = this.world.get().getFriendlyRobotMap(color).get(id).getRobot();

        if (r) {
            command.setKickFlag(EnumKickType.CHIP, this.kickPow.getSecond());
        }

        if (l) {
            command.setKickFlag(EnumKickType.STRAIGHT, this.kickPow.getFirst());
        }

        if (MathHelper.distancePositionToRobot(ballPos, robot) < ROBOT_RAD * 3) {
            // ボールが近ければドリブラーを回す
            command.setDribble(12);
        }

        command.setTargetVel(new Vector2D(-1500.0 * this.state.leftY, -1500.0 * this.state.leftX));
        command.setTargetOmega(-6.0 * FastMath.asin(this.state.rightX) * 2.0 / FastMath.PI);
        this.lastPush[0] = l;
        this.lastPush[1] = r;
        return command;
    }

    // actionが変わった時のロボットの方向を指定する用
    public void setTargetTheta(double theta) {
        this.targetTheta = theta;
    }

    @Override
    public String getName() {
        return "demoControl";
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void setControllerStatus(StatusJoyStick state) {
        this.state = state;
    }

    /**
     * キックパワー設定
     * @param straight ストレート
     * @param chip チップ
     */
    public void setKickPow(int straight, int chip) {
        this.kickPow = new Pair<>(straight, chip);
    }
}
