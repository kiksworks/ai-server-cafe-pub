package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.updater.ConfigManager;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import java.awt.*;
import java.util.List;

public class RoleBoxesCafe extends AbstractGraphicalComponent {
    private IntegratedRobot robot;
    private boolean invert;

    public RoleBoxesCafe(Color color, IntegratedRobot robot, boolean isInvert) {
        super(color);
        this.robot = robot;
        this.invert = isInvert;
    }

    @Override
    public void paint2D(Graphics2D graphics) {
        if (robot.getRole().isEmpty()) return;
        List<Pair<Vector2D, Vector2D>> boxes = robot.getRole().get().getVisualizerBoxes();
        final double ROBOT_RAD = ConfigManager.getInstance().getConfig().robotRadius;

        graphics.setStroke(new BasicStroke(3.0F));
        graphics.setColor(this.color);
        for (Pair<Vector2D, Vector2D> box : boxes) {
            Vector2D v1 = box.getFirst().scalarMultiply(this.invert ? -1.0 : 1.0);
            Vector2D v2 = box.getSecond().scalarMultiply(this.invert ? -1.0 : 1.0);
            graphics.drawRoundRect((int) FastMath.min(v1.getX(), v2.getX()), (int)FastMath.min(v1.getY(), v2.getY()), (int)FastMath.abs(v2.getX() - v1.getX()), (int)FastMath.abs(v2.getY() - v1.getY()), (int)(ROBOT_RAD * 2), (int)(ROBOT_RAD * 2));
        }
    }
}