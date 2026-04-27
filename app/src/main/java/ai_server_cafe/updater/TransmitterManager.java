package ai_server_cafe.updater;

import ai_server_cafe.Main;
import ai_server_cafe.network.radio.EnumRadioType;
import ai_server_cafe.network.transmitter.AbstractTransmitter;
import ai_server_cafe.util.interfaces.WrapperWeakCloneableList;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public final class TransmitterManager {
    private static TransmitterManager instance = null;
    private final List<EnumRadioType> transmitters;

    private TransmitterManager() {
        this.transmitters = new ArrayList<>();
    }

    @Nonnull
    synchronized public WrapperWeakCloneableList<AbstractTransmitter> getTransmitters() {
        List<AbstractTransmitter> transmitterList = new ArrayList<>();
        synchronized (this.transmitters) {
            for (EnumRadioType ert : this.transmitters) {
                if (ert == null) continue;
                try {
                    Object o = ert.getTransmitterClass().getMethod("getInstance").invoke(null);
                    if (o instanceof AbstractTransmitter) {
                        transmitterList.add((AbstractTransmitter) o);
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return new WrapperWeakCloneableList<>(transmitterList);
    }

    synchronized public void setTransmitters(int[] values) {
        for (AbstractTransmitter at : this.getTransmitters().get()) {
            at.terminate();
        }
        synchronized (this.transmitters) {
            this.transmitters.clear();
            for (int value : values) {
                EnumRadioType enumRadioType = EnumRadioType.getFromId(value);
                if (enumRadioType != null) {
                    this.transmitters.add(enumRadioType);
                }
            }
        }
        try {
            for (AbstractTransmitter at : this.getTransmitters().get()) {
                at.startWith(at.getType().getTransmitterPort(), at.getType().getTransmitterAddress(), at.getType().getTransmitterIfAddress());
            }
        } catch (IllegalThreadStateException e) {
            // process実行中にはrestartできないのでauto reboot(instance = null)をかける
            Main.exit(0, true);
        }
    }

    public synchronized static TransmitterManager getInstance() {
        if (instance == null) {
            instance = new TransmitterManager();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }
}
