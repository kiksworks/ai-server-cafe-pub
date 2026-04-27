package ai_server_cafe.gui.component;

import ai_server_cafe.gui.EnumVisibleType;
import ai_server_cafe.gui.interfaces.IComponentCafe;

import javax.swing.*;
import java.awt.*;

public class ScrollPaneCafe<T extends JComponent & IComponentCafe> extends JScrollPane implements IComponentCafe {
    private final T area;

    public ScrollPaneCafe(T area) {
        super(area);
        this.area = area;
        this.setBounds(area.getBounds());
        this.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        this.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        this.setVisible(true);
    }

    @Override
    public EnumVisibleType getDependentType() {
        return this.area.getDependentType();
    }

    @Override
    public Rectangle setPaneSize(int x, int y) {
        Rectangle rect = this.area.setPaneSize(x, y);
        this.setBounds(rect);
        return rect;
    }
}
