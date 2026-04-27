package ai_server_cafe.network.radio;

import ai_server_cafe.config.Config;
import ai_server_cafe.network.transmitter.AbstractTransmitter;
import ai_server_cafe.network.transmitter.BroadcastTransmitter;
import ai_server_cafe.network.transmitter.GrSimTransmitter;
import ai_server_cafe.network.transmitter.KIKSTransmitter;
import ai_server_cafe.network.transmitter.SimulatorTransmitter;
import ai_server_cafe.updater.ConfigManager;

import javax.annotation.Nullable;

public enum EnumRadioType {
    KIKS(0, "kiks", KIKSTransmitter.class),
    GR_SIM(1, "grsim", GrSimTransmitter.class),
    BROADCAST(2, "broad", BroadcastTransmitter.class),
    SIMULATOR(3, "sim",SimulatorTransmitter.class);

    private int id;
    private Class<? extends AbstractTransmitter> clazz;
    private String name;

    EnumRadioType(int id, String name, Class<? extends AbstractTransmitter> clazz) {
        this.id = id;
        this.name = name;
        this.clazz = clazz;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Class<? extends AbstractTransmitter> getTransmitterClass() {
        return this.clazz;
    }

    @Nullable
    public static EnumRadioType getFromId(int id) {
        for (EnumRadioType ert : EnumRadioType.values()) {
            if (ert.getId() == id) {
                return ert;
            }
        }
        return null;
    }

    public String getTransmitterAddress() {
        Config config = ConfigManager.getInstance().getConfig();
        switch (this.id) {
            case 0:
                return config.transmitterAddress;
            case 1:
                return config.grSimAddress;
            case 2:
                return config.broadcastAddress;
            case 3:
                return config.simAddress;
            default:
                return "";
        }
    }

    public String getTransmitterIfAddress() {
        Config config = ConfigManager.getInstance().getConfig();
        if (config.useCommonInterfaceAddress)
            return config.commonInterfaceAddress;
        switch (this.id) {
            case 0, 2:
                return config.transmitterInterfaceAddress;
            case 1:
                return config.grSimInterfaceAddress;
            case 3:
                return config.simInterfaceAddress;
            default:
                return "";
        }
    }

    public int getTransmitterPort() {
        Config config = ConfigManager.getInstance().getConfig();
        switch (this.id) {
            case 0, 2:
                return config.transmitterPort;
            case 1:
                return config.grSimPort;
            case 3:
                return config.simYellowPort;
            default:
                return 0;
        }
    }
}
