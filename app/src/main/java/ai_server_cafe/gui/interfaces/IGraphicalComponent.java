package ai_server_cafe.gui.interfaces;

import java.awt.*;
import java.awt.image.ImageObserver;

public interface IGraphicalComponent {
    public void paint(Graphics2D graphics);
    public default void paint(Graphics2D graphics, ImageObserver observer) {
        paint(graphics);
    }
    public void setSize(int paneWidth, int paneHeight);
    public default void update() {};
}
