package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.awt.*;

public class LineCafe extends AbstractGraphicalComponent {
    private final double startX;
    private final double endX;
    private final double startY;
    private final double endY;
    private final float width;

    public LineCafe(double startX, double startY, double endX, double endY, Color color, float width) {
        super(color);
        this.startX = startX;
        this.endX = endX;
        this.startY = startY;
        this.endY = endY;
        this.width = width;
    }

    public LineCafe(Vector2D start, Vector2D end, Color color, float width) {
        super(color);
        this.startX = start.getX();
        this.endX = end.getX();
        this.startY = start.getY();
        this.endY = end.getY();
        this.width = width;
    }

    @Override
    public void paint2D(Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(this.width));
        graphics.drawLine((int)this.startX, (int)this.startY, (int)this.endX, (int)this.endY);
    }
}
