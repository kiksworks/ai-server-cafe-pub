package ai_server_cafe.gui.interfaces;

import ai_server_cafe.gui.component.MenuCafe;
import ai_server_cafe.util.interfaces.IFuncParam1;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public interface IMenuCafe {
    public IFuncParam1<Void, Vector2D> getFunc();

    public String getName();

    public MenuCafe getSubMenu();
}
