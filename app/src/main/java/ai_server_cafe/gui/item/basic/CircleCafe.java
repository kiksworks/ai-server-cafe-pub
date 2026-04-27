package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;

import java.awt.*;

public class CircleCafe extends AbstractGraphicalComponent {
    private final double x;
    private final double y;
    private final double radius;
    private final boolean isFill;
    private final float stroke;

    public CircleCafe(Color color, double x, double y, double radius, boolean isFill, float stroke) {
        super(color);
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.isFill = isFill;
        this.stroke = stroke;
    }

    @Override
    public void paint2D(Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(this.stroke));
        if (this.isFill) {
            graphics.fillOval((int)(x - radius), (int)(y - radius), 2 * (int)radius, 2 * (int)radius);
        } else {
            graphics.drawOval((int)(x - radius), (int)(y - radius), 2 * (int)radius, 2 * (int)radius);
        }
    }
}
