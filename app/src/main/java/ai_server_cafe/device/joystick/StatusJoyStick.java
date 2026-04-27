package ai_server_cafe.device.joystick;

import ai_server_cafe.util.interfaces.AbstractCloneable;

/**
 * ゲームコントローラによる入力データ
 */
public class StatusJoyStick extends AbstractCloneable {
    public final boolean buttonA;
    public final boolean buttonB;
    public final boolean buttonX;
    public final boolean buttonY;
    public final double leftX;
    public final double leftY;
    public final double rightX;
    public final double rightY;
    public final double leftTrigger;
    public final double rightTrigger;
    public final int pov;
    public final boolean leftButton;
    public final boolean rightButton;

    public static final StatusJoyStick EMPTY = new StatusJoyStick(false, false, false,
            false, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, false, false);

    public StatusJoyStick(boolean buttonA, boolean buttonB, boolean buttonX, boolean buttonY, double leftX, double leftY, double rightX, double rightY, double leftTrigger, double rightTrigger, int pov,
                          boolean leftButton, boolean rightButton) {
        this.buttonA = buttonA;
        this.buttonB = buttonB;
        this.buttonX = buttonX;
        this.buttonY = buttonY;
        this.leftX = leftX;
        this.leftY = leftY;
        this.rightX = rightX;
        this.rightY = rightY;
        this.leftTrigger = leftTrigger;
        this.rightTrigger = rightTrigger;
        this.pov = pov;
        this.leftButton = leftButton;
        this.rightButton = rightButton;
    }

    @Override
    public StatusJoyStick clone() {
        return new StatusJoyStick(this.buttonA, this.buttonB, this.buttonX, this.buttonY, this.leftX, this.leftY, this.rightX, this.rightY, this.leftTrigger, this.rightTrigger, this.pov, this.leftButton, this.rightButton);
    }
}
