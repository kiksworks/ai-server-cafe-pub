package ai_server_cafe.gui.component;

import ai_server_cafe.gui.interfaces.IMenuCafe;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuCafe<T extends IMenuCafe> extends JMenu {
    private int actionX;
    private int actionY;

    public MenuCafe(String name, @Nonnull T[] values) {
        super(name);
        this.setBounds(0, 0, 0, 0);
        this.actionX = 0;
        this.actionY = 0;
        for (T t : values) {
            if (t.getFunc() != null) {
                JMenuItem mi = new JMenuItem(t.getName());
                mi.setActionCommand(t.toString());
                mi.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (t.toString().equals(e.getActionCommand()) && t.getFunc() != null)
                            t.getFunc().function(new Vector2D(actionX, actionY));
                    }
                });
                this.add(mi);
            } else if (t.getSubMenu() != null) {
                t.getSubMenu().setActionCommand(t.toString());
                t.getSubMenu().addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (t.toString().equals(e.getActionCommand())) {
                            t.getSubMenu().setActionPos(actionX, actionY);
                        }
                    }
                });
                this.add(t.getSubMenu());
            }
        }
    }

    public void setActionPos(int x, int y) {
        this.actionX = x;
        this.actionY = y;
    }
}
