package ai_server_cafe.gui.component;

import ai_server_cafe.config.Config;
import ai_server_cafe.gui.EnumVisibleType;
import ai_server_cafe.gui.interfaces.IComponentCafe;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.gui.EnumDisplayType;
import ai_server_cafe.util.interfaces.IFuncParam1;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ComboBoxCafe<T> extends JComboBox<T> implements IComponentCafe {
    private final IFuncParam1<Void, T> onCombo;
    private boolean isExecute = true;
    private EnumVisibleType type;
    private EnumDisplayType xType;
    private EnumDisplayType yType;
    private EnumDisplayType widthType;
    private EnumDisplayType heightType;
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    /**
     * 複数の文字列を格納できるボタン(コンボボックス)
     *
     * @param list 格納する文字列
     * @param x    gui右上からどれだけ右にずらすか
     * @param y    gui右上からどれだけ下にずらすか
     */
    public ComboBoxCafe(@Nonnull T[] list, T init, double x, double y, double width, double height, IFuncParam1<Void, T> onCombo, EnumVisibleType type) {
        this.setVisible(true);
        this.setLayout(null);
        this.setBounds((int) x, (int) y, (int) width, (int) height);
        Config.GUIConfig config = ConfigManager.getInstance().getConfig().guiConfig;
        this.setFont(new Font(config.font, config.fontStyle, config.fontSize));
        this.setEnabled(true);
        this.setSelectedItem(init);
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSelectedItem(getSelectedItem());
            }
        });
        int n = 0;
        for (int i = 0; i < list.length; i++) {
            this.addItem(list[i]);
            if (list[i] == init) {
                n = i;
            }
        }
        this.setSelectedIndex(n);
        this.onCombo = onCombo;
        this.type = type;
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
    @SuppressWarnings("unchecked")
    public void setSelectedItem(Object obj) {
        if (this.onCombo != null && this.isExecute) {
            this.isExecute = false;
            this.onCombo.function((T) obj);
        } else if (this.onCombo != null) {
            this.isExecute = true;
        }
        super.setSelectedItem(obj);
    }

    @Override
    public EnumVisibleType getDependentType() {
        return this.type;
    }

    public ComboBoxCafe<T> setDisplayType(int id, EnumDisplayType type) {
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
