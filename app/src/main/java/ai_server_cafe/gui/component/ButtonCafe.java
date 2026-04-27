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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ButtonCafe extends JButton implements IComponentCafe {
    private final IFuncParam1<Void, Boolean> onButton;
    private final EnumVisibleType type;
    private EnumDisplayType xType;
    private EnumDisplayType yType;
    private EnumDisplayType widthType;
    private EnumDisplayType heightType;
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    /**
     *
     * 押下時に特定の処理を行うボタン
     * @param label ボタンの名前
     * @param x gui右上から何文字分右にずらすか
     * @param y gui右上から何文字分下にずらすか
     * @param onButton 押下時の処理
     * @param type EnumVisibleType
     */
    public ButtonCafe(String label, double x, double y, double width, double height, IFuncParam1<Void, Boolean> onButton, EnumVisibleType type) {
        this.setVisible(true);
        this.setLayout(null);
        this.type = type;
        this.setBounds((int)x, (int)y, (int)width, (int)height);
        this.setSelected(true);
        this.onButton = onButton;
        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        this.setFont(new Font(config.font, config.fontStyle, config.fontSize));
        this.setText(label);
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSelected(!isSelected());
            }
        });
        this.xType = EnumDisplayType.DEFAULT;
        this.yType = EnumDisplayType.DEFAULT;
        this.widthType = EnumDisplayType.DEFAULT;
        this.heightType = EnumDisplayType.DEFAULT;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     *
     * 押下時に特定の処理を行うボタン
     * @param label ボタンの名前
     * @param x gui右上から何文字分右にずらすか
     * @param y gui右上から何文字分下にずらすか
     * @param onButton 押下時の処理
     * @param type EnumVisibleType
     * @param img 画像のpath(src/main/resources/...で指定する)
     */
    public ButtonCafe(String label, double x, double y, double width, double height, IFuncParam1<Void, Boolean> onButton, EnumVisibleType type, ImageIcon img) {
        this.setVisible(true);
        this.setLayout(null);
        this.type = type;
        this.setBounds((int)x, (int)y, (int)width, (int)height);
        this.setSelected(true);
        this.onButton = onButton;
        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        this.setFont(new Font(config.font, config.fontStyle, config.fontSize));
        this.setText(label);
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSelected(!isSelected());
            }
        });
        this.xType = EnumDisplayType.DEFAULT;
        this.yType = EnumDisplayType.DEFAULT;
        this.widthType = EnumDisplayType.DEFAULT;
        this.heightType = EnumDisplayType.DEFAULT;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        if(img != null) {
            this.setIcon(img);
        }
    }

    public ButtonCafe setDisplayType(int id, EnumDisplayType type) {
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
    public void setSelected(boolean value) {
        if(this.onButton != null) {
            this.onButton.function(value);
        }
        super.setSelected(value);
    }

    @Override
    public EnumVisibleType getDependentType() {
        return this.type;
    }

    @Override
    public Rectangle setPaneSize(int x, int y) {
        Rectangle rect = EnumDisplayType.getSize(new Vector2D(this.x, this.y), this.xType, this.yType,
                new Vector2D(this.width, this.height), this.widthType, this.heightType, new Vector2D(x, y));
        this.setBounds(rect);
        return rect;
    }
}