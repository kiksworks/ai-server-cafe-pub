package ai_server_cafe.gui;

import ai_server_cafe.gui.interfaces.AbstractPanelCafe;
import ai_server_cafe.gui.interfaces.IComponentCafe;

import java.awt.*;
import java.awt.event.MouseEvent;

public class TabArea extends AbstractPanelCafe {
    public TabArea(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void initPaint(Graphics2D graphics2D) {
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    @Override
    public boolean isVisibleConfig() {
        return true;
    }

    @Override
    public void onResizePanel(int width, int height) {
        for (Component c : this.getComponents()) {
            if (c instanceof IComponentCafe) {
                ((IComponentCafe)c).setPaneSize(width, height);
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        for (Component c : this.getComponents()) {
            if (c.isVisible() && c instanceof IComponentCafe) {
                ((IComponentCafe)c).mousePressed(e);
            }
        }
    }

    @Override
    public void onResize(int newX, int newY) {
    }
}
