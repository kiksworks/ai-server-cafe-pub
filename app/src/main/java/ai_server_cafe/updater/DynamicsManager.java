package ai_server_cafe.updater;

import ai_server_cafe.config.control.Control;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class DynamicsManager {
    // 全体の速度を上げたり下げたりする 小さくすると同一のkick速度司令に対してpowerが下がる
    private static DynamicsManager instance = null;
    private final Logger logger = LogManager.getLogger("dynamics manager");
    private boolean isDirty;
    private Control dynamics;
    private double defaultRatioA;
    private double defaultRatioB;
    private DynamicsManager() {
        this.isDirty = false;
        this.defaultRatioA = 0.0;
        this.defaultRatioB = 0.0;
        this.load();
    }

    public synchronized static DynamicsManager getInstance() {
        if (instance == null) {
            instance = new DynamicsManager();
        }
        return instance;
    }

    synchronized public void load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            File configDir = new File("config");
            if (!configDir.exists() || !configDir.isDirectory()) {
                configDir.mkdir();
            }
            File configFile = new File("config/dynamics.json");
            boolean exist = !configFile.createNewFile();
            if (!exist) {
                logger.info("created new dynamics config file at {}", configFile.getAbsolutePath());
                this.dynamics = new Control();
                JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(configFile)));
                writer.setIndent("  ");
                gson.toJson(gson.toJsonTree(this.dynamics), writer);
                writer.close();
            } else {
                JsonReader reader = new JsonReader(new BufferedReader(new FileReader(configFile)));
                this.dynamics = gson.fromJson(reader, Control.class);
                reader.close();
            }
        } catch(IOException exception) {
            exception.getStackTrace();
        }
        this.defaultRatioA = ConfigManager.getInstance().getConfig().pathPlannerConfig.withPlannerConfig.defaultAccelCentripetalRatioA;
        this.defaultRatioB = ConfigManager.getInstance().getConfig().pathPlannerConfig.withPlannerConfig.defaultAccelCentripetalRatioB;
    }

    synchronized public void save() {
        if (this.isDirty) {
            // save
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try {
                File configDir = new File("config");
                if (!configDir.exists() || !configDir.isDirectory()) {
                    configDir.mkdir();
                }
                File configFile = new File("config/dynamics.json");
                configFile.createNewFile();
                JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(configFile)));
                writer.setIndent("  ");
                gson.toJson(gson.toJsonTree(this.dynamics), writer);
                writer.close();
                this.isDirty = false;
            } catch (IOException exception) {
                this.logger.error("I/O error occurred : {}", (Object[]) exception.getStackTrace());
            }
        }
    }

    public synchronized double getCentripetalRatio(int id, boolean isTypeB) {
        for (Control.Data data : this.dynamics.data) {
            if (data.hasData && data.id == id && data.isTypeB == isTypeB) {
                return data.centripetalRatio;
            }
        }
        return isTypeB ? this.defaultRatioB : this.defaultRatioA;
    }

    public synchronized void setData(int id, boolean isTypeB, double value) {
        for (Control.Data data : this.dynamics.data) {
            if (data.id == id && data.isTypeB == isTypeB) {
                data.centripetalRatio = value;
                data.hasData = true;
                break;
            }
        }
        this.markDirty();
    }

    public synchronized void clearData(int id, boolean isTypeB) {
        for (Control.Data data : this.dynamics.data) {
            if (data.id == id && data.isTypeB == isTypeB) {
                data.centripetalRatio = 0.0;
                data.hasData = false;
                break;
            }
        }
        this.markDirty();
    }

    public synchronized void markDirty() {
        this.isDirty = true;
    }

    public static void reset() {
        instance = null;
    }
}
