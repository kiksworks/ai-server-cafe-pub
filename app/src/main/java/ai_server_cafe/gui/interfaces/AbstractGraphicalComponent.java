package ai_server_cafe.gui.interfaces;

import ai_server_cafe.util.gui.EnumDisplayType;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.ImageObserver;

public abstract class AbstractGraphicalComponent implements IGraphicalComponent {
    protected final Color color;
    protected final EnumDisplayType xType;
    protected final EnumDisplayType yType;
    protected final EnumDisplayType widthType;
    protected final EnumDisplayType heightType;
    protected int paneWidth;
    protected int paneHeight;
    protected boolean visible;
    public AbstractGraphicalComponent(Color color, EnumDisplayType xType, EnumDisplayType yType, EnumDisplayType widthType, EnumDisplayType heightType) {
        this.color = color;
        this.xType = xType;
        this.yType = yType;
        this.widthType = widthType;
        this.heightType = heightType;
        this.paneWidth = 0;
        this.paneHeight = 0;
        this.visible = false;
    }

    public AbstractGraphicalComponent(Color color) {
        this(color, EnumDisplayType.DEFAULT, EnumDisplayType.DEFAULT, EnumDisplayType.DEFAULT, EnumDisplayType.DEFAULT);
    }

    @Override
    public void paint(@Nonnull Graphics2D graphics, ImageObserver observer) {
        graphics.setColor(this.color);
        this.paint2D(graphics, observer);
    }

    public void paint(Graphics2D graphics) {
        graphics.setColor(this.color);
        this.paint2D(graphics);
    }

    public abstract void paint2D(Graphics2D graphics);

    public void paint2D(Graphics2D graphics, ImageObserver observer) {
        this.paint2D(graphics);
    }

    public void setSize(int width, int height) {
        this.paneWidth = width;
        this.paneHeight = height;
    }
}
