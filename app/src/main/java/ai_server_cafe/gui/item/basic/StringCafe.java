package ai_server_cafe.gui.item.basic;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;
import ai_server_cafe.updater.ConfigManager;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class StringCafe extends AbstractGraphicalComponent {
    private final int x;
    private final int y;
    private final int size;
    private final String str;
    private final boolean isInvert;
    private final double rotation;

    public StringCafe(Color color, int x, int y, String str, int size) {
        this(color, x, y, str, size, true, 0.0);
    }

    public StringCafe(Color color, int x, int y, String str, int size, boolean isInvert, double rotation) {
        super(color);
        this.x = x;
        this.y = y;
        this.str = str;
        this.size = size;
        this.isInvert = isInvert;
        this.rotation = rotation;
    }

    @Override
    public void paint2D(Graphics2D graphics) {
        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        Font font = new Font(config.font, config.fontStyle, size);
        graphics.setFont(font);
        AffineTransform af = graphics.getTransform();
        if (this.isInvert) {
            af.rotate(rotation);
            af.scale(1.0, -1.0);
            graphics.setTransform(af);
        }
        graphics.drawString(str, x, (this.isInvert ? -y : y));
        if (this.isInvert) {
            af.scale(1.0, -1.0);
            af.rotate(-rotation);
            graphics.setTransform(af);
        }
    }
}