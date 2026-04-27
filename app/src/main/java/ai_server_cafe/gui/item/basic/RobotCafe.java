package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.gui.ColorHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.util.FastMath;

import java.awt.*;

public class RobotCafe extends AbstractGraphicalComponent {
    private final TeamColor color;
    private final float stroke;
    private static final double fps = 60.0;
    private final IntegratedRobot robot;

    public RobotCafe(TeamColor color, IntegratedRobot robot) {
        super(color.isYellow() ? ColorHelper.ROBOT_YELLOW : ColorHelper.ROBOT_BLUE);
        this.color = color;
        this.stroke = 4.0F;
        this.robot = robot;
    }

    @Override
    public void paint2D(Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(this.stroke));
        int mouseAngle = 40;
        final double radius = ConfigManager.getInstance().getConfig().robotRadius;
        final double x = robot.getRobot().getX();
        final double y = robot.getRobot().getY();
        final double theta = robot.getRobot().getTheta();
        long tick = (long)(TimeHelper.now() * fps);
        if (this.robot.getCommand().orElse(new Command()).getDribble() != 0) {
            mouseAngle = (int)(tick % 10L) * 24 - (int)(tick % 20L) * 12;
        }

        // 黒で縁取り
        graphics.setColor(Color.black);
        graphics.fillOval((int) (x - 1.2 * radius), (int) (y - 1.2 * radius), 2 * (int)(1.2 * radius), 2 * (int)(1.2 * radius));
        // パックマンを描画
        graphics.setColor(color.isYellow() ? ColorHelper.ROBOT_YELLOW : ColorHelper.ROBOT_BLUE);
        graphics.fillArc((int) (x - radius), (int) (y - radius), 2 * (int) radius, 2 * (int) radius,
                -((int) (theta * 360 / MathHelper.TWO_PI))
                        + FastMath.abs(mouseAngle) / 2,
                360 - FastMath.abs(mouseAngle));
        // キックフラグで白抜き
        if (this.robot.getCommand().orElse(new Command()).getKickFlag().getSecond() != 0) {
            graphics.setColor(Color.white);
            graphics.fillArc((int) (x - 0.8 * radius), (int) (y - 0.8 * radius), 2 * (int) (0.8 * radius), 2 * (int) (0.8 * radius),
                    -((int) (theta * 360 / MathHelper.TWO_PI))
                            + FastMath.abs(mouseAngle) / 2,
                    360 - FastMath.abs(mouseAngle));
        }
        /*
        graphics.setColor(ColorHelper.LINE_WHITE);
        Vector2D pos = robot.getRobot().position();
        Vector2D vel = pos.add(robot.getRobot().velocity());
        graphics.drawLine((int)pos.getX(), (int)pos.getY(),
                (int)vel.getX(), (int)vel.getY());
         */
    }
}
