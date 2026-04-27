package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.updater.ConfigManager;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.awt.*;
import java.util.List;

public class RoleTargetsCafe extends AbstractGraphicalComponent {
    private IntegratedRobot robot;
    private boolean invert;

    public RoleTargetsCafe(Color color, IntegratedRobot robot, boolean isInvert) {
        super(color);
        this.robot = robot;
        this.invert = isInvert;
    }

    @Override
    public void paint2D(Graphics2D graphics) {
        if (robot.getRole().isEmpty()) return;
        List<Vector2D> targets = robot.getRole().get().getVisualizerTargets();

        final Vector2D robotPos = robot.getRobot().position();
        final int radius = (int)ConfigManager.getInstance().getConfig().robotRadius;

        graphics.setStroke(new BasicStroke(30.0F));
        graphics.setColor(this.color);
        for (Vector2D target : targets) {
            target = target.scalarMultiply(this.invert ? -1.0 : 1.0);
            graphics.drawLine((int)robotPos.getX(), (int)robotPos.getY(), (int)target.getX(), (int)target.getY());
        }
        if (!targets.isEmpty()) {
            Vector2D target = targets.getFirst().scalarMultiply(this.invert ? -1.0 : 1.0);
            graphics.drawOval((int)(target.getX() - radius * 4), (int)(target.getY() - radius * 4), radius * 8, radius * 8);
        }
    }
}