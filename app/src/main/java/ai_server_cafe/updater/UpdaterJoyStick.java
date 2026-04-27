package ai_server_cafe.updater;

import ai_server_cafe.device.joystick.StatusJoyStick;
import ai_server_cafe.util.interfaces.WrapperCloneableList;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class UpdaterJoyStick {
    private static UpdaterJoyStick instance = null;
    private List<Controller> controllers;
    private final Logger logger;

    private UpdaterJoyStick() {
        this.controllers = new ArrayList<>();
        this.logger = LogManager.getLogger("updater-game-pad");
    }

    public synchronized static UpdaterJoyStick getInstance() {
        if (instance == null) {
            instance = new UpdaterJoyStick();
        }
        return instance;
    }

    synchronized public void scan() {
        File f = new File("native" + File.separator + "lib" + File.separator + "controller");
        System.setProperty("net.java.games.input.librarypath", f.getAbsolutePath());
        Controller[] cons = ControllerEnvironment.getDefaultEnvironment().getControllers();
        List<Controller> controllerList = new ArrayList<>();
        for (Controller con : cons) {
            if (con.getType() == Controller.Type.GAMEPAD) {
                controllerList.add(con);
            }
        }
        if (!controllerList.isEmpty()) {
            this.controllers = controllerList;
            this.logger.info("{} was detected.", this.controllers);
        } else {
            this.logger.info("No controller was detected. Please plug JoyStick and run process again.");
        }
    }

    @Nonnull
    synchronized public WrapperCloneableList<StatusJoyStick> update() {
        List<StatusJoyStick> result = new ArrayList<>();
        if (!controllers.isEmpty()) {
            for (Controller controller : this.controllers) {
                controller.poll();
                EventQueue queue = controller.getEventQueue();
                Event event = new Event();
                boolean a = false;
                boolean b = false;
                boolean x = false;
                boolean y = false;
                boolean rb = false;
                boolean lb = false;
                double lx = 0.0;
                double ly = 0.0;
                double lt = 0.0;
                double rx = 0.0;
                double ry = 0.0;
                double rt = 0.0;
                int pov = 0;
                while (queue.getNextEvent(event)) {
                    Component component = event.getComponent();
                    if (component.getIdentifier() == Component.Identifier.Button._0 || component.getIdentifier() == Component.Identifier.Button.A) {
                        a = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Button._1 || component.getIdentifier() == Component.Identifier.Button.B) {
                        b = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Button._2 || component.getIdentifier() == Component.Identifier.Button.X) {
                        x = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Button._3 || component.getIdentifier() == Component.Identifier.Button.Y) {
                        y = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Button._4 || component.getIdentifier() == Component.Identifier.Button.LEFT_THUMB) {
                        lb = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Button._5 || component.getIdentifier() == Component.Identifier.Button.RIGHT_THUMB) {
                        rb = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Axis.POV) {
                        pov = (int) (component.getPollData() * 4);
                    }
                }
                for (Component component : controller.getComponents()) {
                    if (component.getIdentifier() == Component.Identifier.Axis.X) {
                        lx = component.getPollData();
                    }
                    if (component.getIdentifier() == Component.Identifier.Axis.Y) {
                        ly = component.getPollData();
                    }
                    if (component.getIdentifier() == Component.Identifier.Axis.Z) {
                        lt = component.getPollData();
                    }
                    if (component.getIdentifier() == Component.Identifier.Axis.RX) {
                        rx = component.getPollData();
                    }
                    if (component.getIdentifier() == Component.Identifier.Axis.RY) {
                        ry = component.getPollData();
                    }
                    if (component.getIdentifier() == Component.Identifier.Axis.RZ) {
                        rt = component.getPollData();
                    }
                    if (component.getIdentifier() == Component.Identifier.Button._0 || component.getIdentifier() == Component.Identifier.Button.A) {
                        a = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Button._1 || component.getIdentifier() == Component.Identifier.Button.B) {
                        b = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Button._2 || component.getIdentifier() == Component.Identifier.Button.X) {
                        x = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Button._3 || component.getIdentifier() == Component.Identifier.Button.Y) {
                        y = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Button._4 || component.getIdentifier() == Component.Identifier.Button.LEFT_THUMB) {
                        lb = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Button._5 || component.getIdentifier() == Component.Identifier.Button.RIGHT_THUMB) {
                        rb = component.getPollData() == 1.0;
                    }
                    if (component.getIdentifier() == Component.Identifier.Axis.POV) {
                        pov = (int) (component.getPollData() * 4);
                    }
                }
                result.add(new StatusJoyStick(a, b, x, y, lx, ly, rx, ry, lt, rt, pov, lb, rb));
            }
        }
        return new WrapperCloneableList<>(result);
    }
}
