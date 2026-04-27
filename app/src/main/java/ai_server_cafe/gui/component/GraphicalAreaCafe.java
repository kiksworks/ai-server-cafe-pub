package ai_server_cafe.gui.component;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.EnumVisibleType;
import ai_server_cafe.gui.interfaces.IComponentCafe;
import ai_server_cafe.gui.interfaces.IGraphicalComponent;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.gui.EnumDisplayType;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.swing.*;
import java.awt.*;

public class GraphicalAreaCafe extends JComponent implements IComponentCafe {
    private final EnumVisibleType type;
    private EnumDisplayType xType;
    private EnumDisplayType yType;
    private EnumDisplayType widthType;
    private EnumDisplayType heightType;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final IGraphicalComponent gc;
    /**
     *
     * テキストを表示する
     * @param x Labelのx座標
     * @param y Labelのy座標
     * @param type EnumVisibleType
     */
    public GraphicalAreaCafe(double x, double y, double width, double height, EnumVisibleType type, IGraphicalComponent gc) {
        this.setVisible(true);
        this.setLayout(null);
        this.type = type;
        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        this.setFont(new Font(config.font, config.fontStyle, config.fontSize));
        this.setBounds((int)x, (int)y, (int)width, (int)height);
        this.xType = EnumDisplayType.DEFAULT;
        this.yType = EnumDisplayType.DEFAULT;
        this.widthType = EnumDisplayType.DEFAULT;
        this.heightType = EnumDisplayType.DEFAULT;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.gc = gc;
    }

    @Override
    public EnumVisibleType getDependentType() {
        return this.type;
    }

    public GraphicalAreaCafe setDisplayType(int id, EnumDisplayType type) {
        if (id == 1)
            this.yType = type;
        else if (id == 2)
            this.widthType = type;
        else if (id == 3)
            this.heightType = type;
        else
            this.xType = type;
        return this;
    }

    @Override
    public void paintComponent(Graphics graphics) {
        if (graphics instanceof Graphics2D) {
            this.gc.paint((Graphics2D) graphics);
        }
    }

    @Override
    public Rectangle setPaneSize(int x, int y) {
        Rectangle rect = EnumDisplayType.getSize(new Vector2D(this.x, this.y), this.xType, this.yType,
                new Vector2D(this.width, this.height), this.widthType, this.heightType, new Vector2D(x, y));
        this.setBounds(rect.x, rect.y, rect.width, rect.height);
        this.gc.setSize(rect.width, rect.height);
        return rect;
    }

    public void update() {
        this.gc.update();
    }
}
