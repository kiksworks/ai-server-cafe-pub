package ai_server_cafe.gui;

import ai_server_cafe.gui.component.MenuCafe;
import ai_server_cafe.gui.interfaces.IMenuCafe;
import ai_server_cafe.model.network.OptionalCommand;
import ai_server_cafe.network.transmitter.AbstractTransmitter;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.TransmitterManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.IFuncParam1;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;

public enum EnumGamePopupMenu implements IMenuCafe {
    LOCATE_BALL(0, "Locate ball here", new IFuncParam1<Void, Vector2D>() {
        @Override
        public Void function(Vector2D vector2D) {
            OptionalCommand oc = new OptionalCommand().setBallPos(vector2D);
            for (AbstractTransmitter at : TransmitterManager.getInstance().getTransmitters().get()) {
                at.sendOptionalCommand(oc);
            }
            return null;
        }
    }, null),
    SET_ABP(1, "Set abp target here", new IFuncParam1<Void, Vector2D>() {
        @Override
        public Void function(Vector2D vector2D) {
            ConfigManager.getInstance().getEditableConfig().localRefBoxConfig.ballPlacePosX = vector2D.getX();
            ConfigManager.getInstance().getEditableConfig().localRefBoxConfig.ballPlacePosY = vector2D.getY();
            ConfigManager.getInstance().setGuiNeedManualUpdate(true);
            return null;
        }
    }, null),
    PUT_YELLOW(2, "Yellow robot here", null, new MenuCafe<>("Set yellow robot here", getRobotPlace(TeamColor.YELLOW))),
    PUT_BLUE(3, "Blue robot here", null, new MenuCafe<>("Set blue robot here", getRobotPlace(TeamColor.BLUE)));

    private final int id;
    private final String name;
    private final IFuncParam1<Void, Vector2D> function;
    private final MenuCafe subMenu;

    EnumGamePopupMenu(int id, String name, IFuncParam1<Void, Vector2D> function, MenuCafe subMenu) {
        this.id = id;
        this.name = name;
        this.function = function;
        this.subMenu = subMenu;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public MenuCafe getSubMenu() {
        return this.subMenu;
    }

    public EnumGamePopupMenu getFromId(int id) {
        for (EnumGamePopupMenu type : EnumGamePopupMenu.values()) {
            if (type.id == id) {
                return type;
            }
        }
        return LOCATE_BALL;
    }

    @Override
    public IFuncParam1<Void, Vector2D> getFunc() {
        return this.function;
    }

    @Nonnull
    private static IMenuCafe[] getRobotPlace(TeamColor color) {
        IMenuCafe[] mc = new IMenuCafe[ConfigManager.MAX_ROBOTS];
        for (int id = 0; id < ConfigManager.MAX_ROBOTS; id++) {
            int finalId = id;
            mc[id] = new IMenuCafe() {
                @Override
                public IFuncParam1<Void, Vector2D> getFunc() {
                    return new IFuncParam1<Void, Vector2D>() {
                        @Override
                        public Void function(Vector2D vector2D) {
                            OptionalCommand oc = new OptionalCommand().setRobotPos(color, finalId, vector2D);
                            for (AbstractTransmitter at : TransmitterManager.getInstance().getTransmitters().get()) {
                                at.sendOptionalCommand(oc);
                            }
                            return null;
                        }
                    };
                }

                @Override
                public String getName() {
                    return String.valueOf(finalId);
                }

                @Override
                public MenuCafe getSubMenu() {
                    return null;
                }
            };
        }
        return mc;
    }

    public static void setBallVel(Vector2D pos, Vector2D vel) {
        OptionalCommand oc = new OptionalCommand().setBallPos(pos).setBallVel(vel);
        for (AbstractTransmitter at : TransmitterManager.getInstance().getTransmitters().get()) {
            at.sendOptionalCommand(oc);
        }
    }
}
