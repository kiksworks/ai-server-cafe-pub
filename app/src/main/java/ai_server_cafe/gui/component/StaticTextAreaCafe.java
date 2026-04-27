package ai_server_cafe.gui.component;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.EnumVisibleType;
import ai_server_cafe.gui.interfaces.IComponentCafe;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.gui.ColorHelper;
import ai_server_cafe.util.gui.EnumDisplayType;
import ai_server_cafe.util.interfaces.IFuncParam1;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.swing.*;
import java.awt.*;

public class StaticTextAreaCafe extends JTextArea implements IComponentCafe {
    private final EnumVisibleType type;
    private String lastBuffer;
    private final IFuncParam1<String, String> function;
    private EnumDisplayType xType;
    private EnumDisplayType yType;
    private EnumDisplayType widthType;
    private EnumDisplayType heightType;
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public StaticTextAreaCafe(double x, double y, double width, double height, IFuncParam1<String, String> updateString, EnumVisibleType type) {
        super(1,  1);
        this.setBounds((int) x, (int) y, (int) width, (int) height);
        this.setAutoscrolls(false);
        this.setEnabled(false);
        this.type = type;
        this.setVisible(true);
        this.setLayout(null);
        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        this.setDisabledTextColor(ColorHelper.GROUND_BLACK);
        this.setFont(new Font(config.font, config.fontStyle, config.fontSize));
        this.lastBuffer = "";
        this.function = updateString;
        this.xType = EnumDisplayType.DEFAULT;
        this.yType = EnumDisplayType.DEFAULT;
        this.widthType = EnumDisplayType.DEFAULT;
        this.heightType = EnumDisplayType.DEFAULT;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public EnumVisibleType getDependentType() {
        return this.type;
    }

    public void paintComponent(Graphics graphics) {
        String buf = this.function.function(this.lastBuffer);
        if (!buf.equals(this.lastBuffer)) {
            this.setText(buf);
            this.lastBuffer = buf;
        }
        super.paintComponent(graphics);
    }

    public StaticTextAreaCafe setDisplayType(int id, EnumDisplayType type) {
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
    public Rectangle setPaneSize(int x, int y) {
        Rectangle rect = EnumDisplayType.getSize(new Vector2D(this.x, this.y), this.xType, this.yType,
                new Vector2D(this.width, this.height), this.widthType, this.heightType, new Vector2D(x, y));
        this.setBounds(rect);
        return rect;
    }
}
