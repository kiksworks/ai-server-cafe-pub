package ai_server_cafe.updater;

import ai_server_cafe.Main;
import ai_server_cafe.network.radio.EnumRadioType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ConfigBuffers {
    private final boolean[] yellowActives;
    private final boolean[] blueActives;
    private final boolean[] radios;
    private final boolean[] yellowObstacles;
    private final boolean[] blueObstacles;
    private String interfaceAddress;
    private int visionPort;
    private static ConfigBuffers instance = null;
    private ConfigBuffers() {
        this.yellowActives = new boolean[ConfigManager.MAX_ROBOTS + 1];
        Arrays.fill(this.yellowActives, false);
        this.blueActives = new boolean[ConfigManager.MAX_ROBOTS + 1];
        Arrays.fill(this.blueActives, false);
        this.radios = new boolean[EnumRadioType.values().length + 1];
        Arrays.fill(this.radios, false);
        this.interfaceAddress = "";
        this.visionPort = 0;
        this.blueObstacles = new boolean[ConfigManager.MAX_ROBOTS + 1];
        Arrays.fill(this.blueObstacles, false);
        this.yellowObstacles = new boolean[ConfigManager.MAX_ROBOTS + 1];
        Arrays.fill(yellowObstacles, false);
        this.sync();
    }

    private void sync() {
        for (int id : ConfigManager.getInstance().getConfig().getActiveRobots(TeamColor.YELLOW)) {
            this.yellowActives[id] = true;
        }
        for (int id : ConfigManager.getInstance().getConfig().getActiveRobots(TeamColor.BLUE)) {
            this.blueActives[id] = true;
        }
        for (int id : ConfigManager.getInstance().getConfig().visibility.yellowObstacle) {
            this.yellowObstacles[id] = true;
        }
        for (int id : ConfigManager.getInstance().getConfig().visibility.blueObstacle) {
            this.blueObstacles[id] = true;
        }
        for (int id : ConfigManager.getInstance().getConfig().radioTypes) {
            this.radios[id] = true;
        }
        this.visionPort = ConfigManager.getInstance().getConfig().visionPort;
        this.interfaceAddress = ConfigManager.getInstance().getConfig().commonInterfaceAddress;
    }

    synchronized public void sendYellowActivesToConfig() {
        if (!this.yellowActives[this.yellowActives.length - 1]) return;
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < this.yellowActives.length - 1; i++) {
            if (this.yellowActives[i]) result.add(i);
        }
        ConfigManager.getInstance().getEditableConfig().activeYellowRobots = MathHelper.toArray(result);
        ConfigManager.getInstance().setNeedReset(TeamColor.YELLOW, true);
        ConfigManager.getInstance().markDirty();
        this.yellowActives[this.yellowActives.length - 1] = false;
    }

    synchronized public void sendBlueActivesToConfig() {
        if (!this.blueActives[this.blueActives.length - 1]) return;
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < this.blueActives.length - 1; i++) {
            if (this.blueActives[i]) result.add(i);
        }
        ConfigManager.getInstance().getEditableConfig().activeBlueRobots = MathHelper.toArray(result);
        ConfigManager.getInstance().setNeedReset(TeamColor.BLUE, true);
        ConfigManager.getInstance().markDirty();
        this.blueActives[this.blueActives.length - 1] = false;
    }

    synchronized public void sendYellowObstacleToConfig() {
        if (!this.yellowObstacles[this.yellowObstacles.length - 1]) return;
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < this.yellowObstacles.length - 1; i++) {
            if (this.yellowObstacles[i]) result.add(i);
        }
        ConfigManager.getInstance().getEditableConfig().visibility.yellowObstacle = MathHelper.toArray(result);
        ConfigManager.getInstance().markDirty();
        this.yellowObstacles[this.yellowObstacles.length - 1] = false;
    }

    synchronized public void sendBlueObstacleToConfig() {
        if (!this.blueObstacles[this.blueObstacles.length - 1]) return;
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < this.blueObstacles.length - 1; i++) {
            if (this.blueObstacles[i]) result.add(i);
        }
        ConfigManager.getInstance().getEditableConfig().visibility.blueObstacle = MathHelper.toArray(result);
        ConfigManager.getInstance().markDirty();
        this.blueObstacles[this.blueObstacles.length - 1] = false;
    }

    synchronized public void sendRadiosToConfig() {
        if (this.radios[this.radios.length - 1]) {
            List<Integer> result = new ArrayList<>();
            for (int i = 0; i < this.radios.length - 1; i++) {
                if (this.radios[i]) result.add(i);
            }
            ConfigManager.getInstance().getEditableConfig().radioTypes = MathHelper.toArray(result);
            this.radios[this.radios.length - 1] = false;
        }
        ConfigManager.getInstance().getEditableConfig().commonInterfaceAddress = this.interfaceAddress;
        ConfigManager.getInstance().getEditableConfig().visionPort = this.visionPort;
        ConfigManager.getInstance().markDirty();
        Main.exit(0, true);
    }

    synchronized public void setYellowActive(int id, boolean value) {
        if (this.yellowActives[id] != value) {
            this.yellowActives[this.yellowActives.length - 1] = true;
        }
        this.yellowActives[id] = value;
    }

    synchronized public void setBlueActive(int id, boolean value) {
        if (this.blueActives[id] != value) {
            this.blueActives[this.blueActives.length - 1] = true;
        }
        this.blueActives[id] = value;
    }

    synchronized public void setYellowObstacle(int id, boolean value) {
        if (this.yellowObstacles[id] != value) {
            this.yellowObstacles[this.yellowObstacles.length - 1] = true;
        }
        this.yellowObstacles[id] = value;
    }

    synchronized public void setBlueObstacle(int id, boolean value) {
        if (this.blueObstacles[id] != value) {
            this.blueObstacles[this.blueObstacles.length - 1] = true;
        }
        this.blueObstacles[id] = value;
    }

    synchronized public void setRadioActive(int id, boolean value) {
        if (this.radios[id] != value) {
            this.radios[this.radios.length - 1] = true;
        }
        this.radios[id] = value;
    }

    synchronized public void setInterfaceAddress(String common) {
        this.interfaceAddress = common;
    }

    synchronized public void setVisionPort(int port) {
        this.visionPort = port;
    }

    synchronized public boolean isUpdatedYellowActive() {
        return this.yellowActives[this.yellowActives.length - 1];
    }

    synchronized public boolean isUpdatedBlueActive() {
        return this.blueActives[this.blueActives.length - 1];
    }

    synchronized public boolean isUpdatedRadioActive() {
        return this.radios[this.radios.length - 1];
    }

    public static synchronized ConfigBuffers getInstance() {
        if (instance == null) {
            instance = new ConfigBuffers();
        }
        return instance;
    }
}
