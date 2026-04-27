package ai_server_cafe.gui.component;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.EnumVisibleType;
import ai_server_cafe.gui.interfaces.IComponentCafe;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.gui.EnumDisplayType;
import ai_server_cafe.util.interfaces.IFuncParam1;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.swing.*;
import java.awt.*;

public class DynamicLabelCafe extends JLabel implements IComponentCafe {
    private final EnumVisibleType type;
    private EnumDisplayType xType;
    private EnumDisplayType yType;
    private EnumDisplayType widthType;
    private EnumDisplayType heightType;
    private final double x;
    private final double y;
    private double width;
    private final double height;
    private int offset;
    IFuncParam1<String, String> updateString;
    private String lastLabel;
    /**
     *
     * テキストを表示する
     * @param label 表示する文字列の初期値
     * @param x Labelのx座標
     * @param y Labelのy座標
     * @param updateString Labelをupdateする
     * @param type EnumVisibleType
     */
    public DynamicLabelCafe(String label, double x, double y, IFuncParam1<String, String> updateString, EnumVisibleType type) {
        this.setVisible(true);
        this.setLayout(null);
        this.type = type;
        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        this.setFont(new Font(config.font, config.fontStyle, config.fontSize));
        this.setBounds((int)x, (int)y, this.getFontMetrics(this.getFont()).stringWidth(label), this.getFontMetrics(this.getFont()).getHeight());
        this.setText(label);
        this.xType = EnumDisplayType.DEFAULT;
        this.yType = EnumDisplayType.DEFAULT;
        this.widthType = EnumDisplayType.DEFAULT;
        this.heightType = EnumDisplayType.DEFAULT;
        this.x = x;
        this.y = y;
        this.width = this.getFontMetrics(this.getFont()).stringWidth(label);
        this.height = this.getFontMetrics(this.getFont()).getHeight();
        this.updateString = updateString;
        this.lastLabel = "";

    }
    @Override
    public EnumVisibleType getDependentType() {
        return this.type;
    }

    public void paintComponent(Graphics graphics) {
        String label = this.updateString.function(this.lastLabel);
        if (!label.equals(this.lastLabel)) {
            this.setText(label);
            this.width = this.getFontMetrics(this.getFont()).stringWidth(label) + 64;
            this.lastLabel = label;
            this.setSize((int) this.width, (int) this.height);
        }
        super.paintComponent(graphics);
    }

    public DynamicLabelCafe setDisplayType(int id, EnumDisplayType type) {
        if (id == 1)
            this.yType = type;
        else if (id == 2)
            this.widthType = EnumDisplayType.DEFAULT;
        else if (id == 3)
            this.heightType = EnumDisplayType.DEFAULT;
        else
            this.xType = type;
        return this;
    }

    @Override
    public Rectangle setPaneSize(int x, int y) {
        Rectangle rect = EnumDisplayType.getSize(new Vector2D(this.x, this.y), this.xType, this.yType,
                new Vector2D(this.width, this.height), this.widthType, this.heightType, new Vector2D(x, y));
        this.setBounds(rect.x + this.offset, rect.y, rect.width, rect.height);
        return rect;
    }

    public DynamicLabelCafe setOffset(int x) {
        this.offset = x;
        return this;
    }
}
