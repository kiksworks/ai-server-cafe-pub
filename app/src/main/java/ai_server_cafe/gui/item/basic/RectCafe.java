package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.util.gui.EnumDisplayType;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.awt.*;

public class RectCafe extends AbstractGraphicalComponent {
    private final double startX;
    private final double startY;
    private final double width;
    private final double height;
    private final float stroke;
    private final boolean isFill;

    public RectCafe(double startX, double startY, double width, double height, Color color, float stroke,
                    boolean isFill, EnumDisplayType xType, EnumDisplayType yType, EnumDisplayType widthType, EnumDisplayType heightType) {
        super(color, xType, yType, widthType, heightType);
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
        this.stroke = stroke;
        this.isFill = isFill;
    }

    public RectCafe(double startX, double startY, double width, double height, Color color, float stroke) {
        super(color);
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
        this.stroke = stroke;
        this.isFill = false;
    }

    public RectCafe(double startX, double startY, double width, double height, Color color) {
        super(color);
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
        this.stroke = 0.0F;
        this.isFill = true;
    }

    @Override
    public void paint2D(Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(this.stroke));
        Rectangle rect = EnumDisplayType.getSize(new Vector2D(this.startX, this.startY), this.xType, this.yType,
                new Vector2D(this.width, this.height), this.widthType, this.heightType, new Vector2D(this.paneWidth, this.paneHeight));
        if(isFill) {
            graphics.fillRect(rect.x, rect.y, rect.width, rect.height);
        } else {
            graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
        }
    }
}
