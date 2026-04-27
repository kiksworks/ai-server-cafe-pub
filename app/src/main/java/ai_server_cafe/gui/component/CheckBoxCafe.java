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

public class CheckBoxCafe extends JCheckBox implements IComponentCafe {
    public final IFuncParam1<Void, Boolean> onCheck;
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
     * チェックボックス
     * @param label チェックボックス名
     * @param x gui右上から何文字分右にずらすか
     * @param y gui右上から何文字分下にずらすか
     * @param type EnumVisibleType
     */
    public CheckBoxCafe(String label, boolean init, double x, double y, double width, double height, IFuncParam1<Void, Boolean> onCheck, EnumVisibleType type) {
        this.setVisible(true);
        this.setLayout(null);
        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        this.setFont(new Font(config.font, config.fontStyle, config.fontSize));
        this.type =type;
        this.setBounds((int) x, (int) y, (int) width, (int) height);
        this.setText(label);
        this.onCheck = onCheck;
        this.setSelected(init);
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSelected(isSelected());
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

    @Override
    public void setSelected(boolean value) {
        if(this.onCheck != null) {
            this.onCheck.function(value);
        }
        super.setSelected(value);
    }

    @Override
    public EnumVisibleType getDependentType() {
        return this.type;
    }

    public CheckBoxCafe setDisplayType(int id, EnumDisplayType type) {
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