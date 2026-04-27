package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PointsCafe extends AbstractGraphicalComponent {
    private final java.util.List<Vector2D> points;
    private final Vector2D start;
    private final double radius;
    public PointsCafe(Color color, Optional<List<Vector2D>> points, Vector2D start, double radius) {
        super(color);
        this.points = points.isPresent() ? points.get() : new ArrayList<>();
        this.radius = radius;
        this.start = start;
    }

    @Override
    public void paint2D(@Nonnull Graphics2D graphics) {
        graphics.fillOval((int)(start.getX() - radius), (int)(start.getY() - radius), 2 * (int)radius, 2 * (int)radius);
        for (Vector2D point : this.points) {
            graphics.fillOval((int)(point.getX() - radius), (int)(point.getY() - radius), 2 * (int)radius, 2 * (int)radius);
        }
    }
}
