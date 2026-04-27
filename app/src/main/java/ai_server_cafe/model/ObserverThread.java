package ai_server_cafe.model;

import ai_server_cafe.model.network.OptionalCommand;
import ai_server_cafe.network.transmitter.AbstractTransmitter;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.TransmitterManager;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class ObserverThread extends AbstractLoopThreadCafe {
    private static ObserverThread instance = null;
    private double lastDate;

    protected ObserverThread() {
        super("observer-thread");
        this.lastDate = TimeHelper.now();
    }

    public static ObserverThread getInstance() {
        if(instance == null) {
            instance = new ObserverThread();
        }
        return instance;
    }

    @Override
    protected void loop() {
        final double now = TimeHelper.now();
        if(now - this.lastDate > ConfigManager.getInstance().getCycleTime()) {
            if(UpdaterWorld.getInstance().isBallOutSide()) {
                OptionalCommand oc = new OptionalCommand();
                oc.setBallPos(Vector2D.ZERO);
                oc.setBallVel(Vector2D.ZERO);
                for (AbstractTransmitter at : TransmitterManager.getInstance().getTransmitters().get()) {
                    at.sendOptionalCommand(oc);
                }
            }
        }
    }

    @Override
    protected void init() {
        this.lastDate = TimeHelper.now();
    }

    public static void reset() {
        instance = null;
    }
}
