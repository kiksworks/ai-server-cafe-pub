package ai_server_cafe.gui.interfaces;

import ai_server_cafe.util.interfaces.IFuncParam1;

import java.awt.*;
import java.util.List;

public interface IContainerCafe {
    /**
     * set graphical contents. should be called at loop.
     */
    public void setGraphicalContents(List<IGraphicalComponent> graphicalContents);

    /**
     * set action contents. should be called at init.
      */
    public void addActionContents(List<Component> component);

    public void doToActionContents(IFuncParam1<Void, Component> func);

    public void onResize(int newX, int newY);
}
