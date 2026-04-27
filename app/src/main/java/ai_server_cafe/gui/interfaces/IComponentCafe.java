package ai_server_cafe.gui.interfaces;

import ai_server_cafe.gui.EnumVisibleType;

import java.awt.*;
import java.awt.event.MouseEvent;

public interface IComponentCafe {
    public EnumVisibleType getDependentType();
    public Rectangle setPaneSize(int x, int y);
    public default void mousePressed(MouseEvent e) {}
    public default void update() {
    };
    public default void updateManually() {}
}
