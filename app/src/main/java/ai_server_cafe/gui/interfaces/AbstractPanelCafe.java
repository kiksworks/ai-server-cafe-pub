package ai_server_cafe.gui.interfaces;

import ai_server_cafe.gui.EnumGamePopupMenu;
import ai_server_cafe.util.interfaces.IFuncParam1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPanelCafe extends JPanel implements IContainerCafe {
	private final List<IGraphicalComponent> components;
    private final List<Component> actionComponents;

    public AbstractPanelCafe(int x, int y, int width, int height) {
        super();
        this.setLayout(null);
        this.setBounds(x, y, width, height);
        this.components = new ArrayList<IGraphicalComponent>();
        this.actionComponents = new ArrayList<>();
        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                onResizePanel(componentEvent.getComponent().getWidth(), componentEvent.getComponent().getHeight());
            }});
        IFuncParam1<Void, EnumGamePopupMenu> guiCombo = new IFuncParam1<Void, EnumGamePopupMenu>() {
            @Override
            public Void function(EnumGamePopupMenu type) {
                return null;
            }
        };
        this.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                AbstractPanelCafe.this.mouseClicked(e);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                AbstractPanelCafe.this.mousePressed(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                AbstractPanelCafe.this.mouseReleased(e);
            }
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        });
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (graphics instanceof Graphics2D && this.isVisibleConfig()) {
            this.initPaint((Graphics2D) graphics);
            synchronized (this.components) {
                for (IGraphicalComponent gc : this.components) {
                    gc.setSize(this.getWidth(), this.getHeight());
                    gc.paint((Graphics2D) graphics, this);
                }
            }
        }
    }

    public void setGraphicalContents(List<IGraphicalComponent> graphicalContents) {
        synchronized (this.components) {
            this.components.clear();
            this.components.addAll(graphicalContents);
        }
    }

    public abstract void initPaint(Graphics2D graphics2D);

    @Override
    public void addActionContents(List<Component> component) {
        synchronized (this.actionComponents) {
            this.actionComponents.addAll(component);
        }
        for (Component c : component) {
            if (c instanceof IComponentCafe) {
                ((IComponentCafe)c).setPaneSize(this.getWidth(), this.getHeight());
            }
            this.add(c);
        }
    }

    @Override
    public void doToActionContents(IFuncParam1<Void, Component> func) {
        synchronized (this.actionComponents) {
            for (Component c : this.actionComponents) {
                func.function(c);
            }
        }
    }

    public abstract boolean isVisibleConfig();

    public abstract void onResizePanel(int width, int height);

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}
