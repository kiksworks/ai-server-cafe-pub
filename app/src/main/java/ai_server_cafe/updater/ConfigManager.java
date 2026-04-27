package ai_server_cafe.updater;

import ai_server_cafe.config.Config;
import ai_server_cafe.config.ConfigSettings;
import ai_server_cafe.gui.EnumVisibleType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.gui.EnumLogLevel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

public final class ConfigManager {
    private static ConfigManager instance = null;
    public static final int MAX_ROBOTS = 16;
    private Config config;
    private ConfigSettings configSettings;
    private Optional<Boolean> isReplayGoalYellowOfPositive = Optional.empty();
    private boolean start = false;
    private boolean useLocalRef = false;
    private boolean needResetRecord = false;
    private boolean needResetYellow = false;
    private boolean needResetBlue = false;
    private boolean logViewFreeze = false;
    private EnumLogLevel logLevel = EnumLogLevel.INFO;
    private boolean isDirty = false;
    private EnumVisibleType guiVisible = EnumVisibleType.HOME;
    private boolean isGuiNeedManualUpdate = false;
    private final Logger logger = LogManager.getLogger("config manager");

    private ConfigManager() {
        this.load();
    }

    /**
     * 起動時にconfigを読み込む
     */
    synchronized public void load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            File configDir = new File("config");
            if (!configDir.exists() || !configDir.isDirectory()) {
                configDir.mkdir();
            }
            File configSettingsFile = new File("config/configSettings.json");
            boolean exist = !configSettingsFile.createNewFile();
            if (!exist) {
                logger.info("created new config settings file at {}", configSettingsFile.getAbsolutePath());
                this.configSettings = new ConfigSettings();
                saveSettings();
                File configFile = new File("config/" + this.configSettings.configName);
                if (configFile.createNewFile()) {
                    logger.info("created new config file at {}", configFile.getAbsolutePath());
                    this.config = new Config();
                    JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(configFile)));
                    writer.setIndent("  ");
                    gson.toJson(gson.toJsonTree(this.config), writer);
                    writer.close();
                } else {
                    loadFrom(this.configSettings.configName);
                }
            } else {
                JsonReader settingsReader = new JsonReader(new BufferedReader(new FileReader(configSettingsFile)));
                this.configSettings = gson.fromJson(settingsReader, ConfigSettings.class);
                settingsReader.close();
                loadFrom(this.configSettings.configName);
            }
        } catch (IOException exception) {
            exception.getStackTrace();
        }
    }

    /**
     * ファイルを指定してConfigを読み込む
     *
     * @param name 読み込むファイル名(拡張子は省略可能)
     */
    synchronized public void loadFrom(String name) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            File configDir = new File("config");
            if (!configDir.exists() || !configDir.isDirectory()) {
                configDir.mkdir();
            }
            String fileName = name.endsWith(".json") ? name : name + ".json";
            File configFile = new File("config/" + fileName);
            this.configSettings.configName = fileName;
            if (configFile.exists()) {
                JsonReader reader = new JsonReader(new BufferedReader(new FileReader(configFile)));
                this.config = gson.fromJson(reader, Config.class);
                reader.close();
            } else {
                logger.info("{}: No such file", configFile.getAbsolutePath());
            }
            this.isDirty = false;
        } catch (IOException exception) {
            exception.getStackTrace();
        }
    }

    /**
     * 開いているconfigに上書き保存
     */
    synchronized public void save() {
        if (this.isDirty) {
            // save
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try {
                File configDir = new File("config");
                if (!configDir.exists() || !configDir.isDirectory()) {
                    configDir.mkdir();
                }
                File configFile = new File("config/" + this.configSettings.configName);
                configFile.createNewFile();
                JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(configFile)));
                writer.setIndent("  ");
                gson.toJson(gson.toJsonTree(this.config), writer);
                writer.close();
                this.isDirty = false;
                saveSettings();
            } catch (IOException exception) {
                this.logger.error("I/O error occurred : {}", (Object[]) exception.getStackTrace());
            }
            this.isDirty = false;
        }
    }

    /**
     * 名前をつけて保存
     *
     * @param name 保存するファイル名(拡張子は省略可能)
     */
    synchronized public void saveAs(String name) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File configDir = new File("config");
        if (!configDir.exists() || !configDir.isDirectory()) {
            configDir.mkdir();
        }
        String fileName = name.endsWith(".json") ? name : name + ".json";
        this.configSettings.configName = fileName;
        save();
    }

    synchronized public void saveSettings() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            File settingsFile = new File("config/configSettings.json");
            JsonWriter settingsWriter = new JsonWriter(new BufferedWriter(new FileWriter(settingsFile)));
            settingsWriter.setIndent("  ");
            gson.toJson(gson.toJsonTree(this.configSettings), settingsWriter);
            settingsWriter.close();
        } catch (IOException exception) {
            this.logger.error("I/O error occurred : {}", (Object[]) exception.getStackTrace());
        }
    }


    public synchronized static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public synchronized void replayStop() {
        this.isReplayGoalYellowOfPositive = Optional.empty();
    }

    public synchronized double getCycleTime() {
        return this.config.getCycleTime();
    }

    public synchronized double getSendCycleTime() {
        return this.config.getSendCycleTime();
    }

    public synchronized double getFrameTime() {
        return this.config.getFrameTime();
    }

    public synchronized void markDirty() {
        this.isDirty = true;
    }

    public synchronized boolean isDirty() {
        return this.isDirty;
    }

    synchronized public Config getConfig() {
        if (this.config == null) {
            this.load();
        }
        return this.config.clone();
    }

    synchronized public Config getEditableConfig() {
        if (this.config == null) {
            this.load();
        }
        return this.config;
    }

    synchronized public ConfigSettings getConfigSettings() {
        if (this.configSettings == null) {
            this.load();
        }
        return this.configSettings;
    }

    synchronized public boolean isStart() {
        return this.start;
    }

    synchronized public void setStart(boolean value) {
        this.start = value;
    }

    synchronized public void setNeedResetRecord(boolean value) {
        this.needResetRecord = value;
    }

    synchronized public boolean isNeedResetRecord() {
        return this.needResetRecord;
    }

    synchronized public void setNeedReset(@Nonnull TeamColor color, boolean value) {
        if (color.isYellow()) {
            this.needResetYellow = value;
        } else {
            this.needResetBlue = value;
        }
    }

    synchronized public boolean isNeedReset(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            return this.needResetYellow;
        } else {
            return this.needResetBlue;
        }
    }

    synchronized public EnumVisibleType getGuiVisibleType() {
        return this.guiVisible;
    }

    synchronized public void setGuiVisibleType(EnumVisibleType type) {
        this.guiVisible = type;
    }

    synchronized public boolean isUseLocalRef() {
        return this.useLocalRef;
    }

    synchronized public void setUseLocalRef(boolean value) {
        this.useLocalRef = value;
    }

    synchronized public boolean isLogViewFreeze() {
        return this.logViewFreeze;
    }

    synchronized public void setLogViewFreeze(boolean value) {
        this.logViewFreeze = value;
    }

    synchronized public void setLogLevel(EnumLogLevel level) {
        this.logLevel = level;
    }

    synchronized public EnumLogLevel getLogLevel() {
        return this.logLevel;
    }

    synchronized public void setGuiNeedManualUpdate(boolean value) {
        this.isGuiNeedManualUpdate = value;
    }

    synchronized public boolean isGuiNeedManualUpdate() {
        return this.isGuiNeedManualUpdate;
    }

    synchronized public void setIsReplayGoalYellowOfPositive(boolean isReplayGoalYellowOfPositive) {
        this.isReplayGoalYellowOfPositive = Optional.of(isReplayGoalYellowOfPositive);
    }

    synchronized public boolean isInvert(@Nonnull TeamColor color) {
        if(this.isReplayGoalYellowOfPositive.isPresent()) {
            return (color.isYellow() && this.isReplayGoalYellowOfPositive.get())
                    || (!color.isYellow() && !this.isReplayGoalYellowOfPositive.get());
        }
        return (color.isYellow() && this.config.isGoalOfYellowPositive)
                || (!color.isYellow() && !this.config.isGoalOfYellowPositive);
    }

    public static void reset() {
        instance = null;
    }
}
