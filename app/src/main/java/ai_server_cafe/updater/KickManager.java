package ai_server_cafe.updater;

import ai_server_cafe.config.kick.Kick;
import ai_server_cafe.util.interfaces.Tuple;
import ai_server_cafe.util.interfaces.WrapperWeakCloneable;
import ai_server_cafe.util.math.MathHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class KickManager {
    // 全体の速度を上げたり下げたりする 小さくすると同一のkick速度司令に対してpowerが下がる
    private static final double KICK_FACTOR = 1.0;
    private static KickManager instance = null;
    private final Logger logger = LogManager.getLogger("kick converter");
    private boolean isDirty;
    private Kick kick;
    private KickManager() {
        this.isDirty = false;
        this.load();
    }

    public synchronized static KickManager getInstance() {
        if (instance == null) {
            instance = new KickManager();
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
            File configFile = new File("config/kick.json");
            boolean exist = !configFile.createNewFile();
            if (!exist) {
                logger.info("created new kick config file at {}", configFile.getAbsolutePath());
                this.kick = new Kick();
                JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(configFile)));
                writer.setIndent("  ");
                gson.toJson(gson.toJsonTree(this.kick), writer);
                writer.close();
            } else {
                JsonReader reader = new JsonReader(new BufferedReader(new FileReader(configFile)));
                this.kick = gson.fromJson(reader, Kick.class);
                reader.close();
            }
        } catch(IOException exception) {
            exception.getStackTrace();
        }
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
                File configFile = new File("config/kick.json");
                configFile.createNewFile();
                JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(configFile)));
                writer.setIndent("  ");
                gson.toJson(gson.toJsonTree(this.kick), writer);
                writer.close();
                this.isDirty = false;
            } catch (IOException exception) {
                this.logger.error("I/O error occurred : {}", (Object[]) exception.getStackTrace());
            }
        }
    }

    public synchronized void setKickParam(int id, boolean isChip, double c0, double c1, double c2) {
        int index;
        for (index = 0; index < this.kick.data.length; index++) {
            if (this.kick.data[index].id == id && this.kick.data[index].chip == isChip) {
                break;
            }
        }
        if (index >= this.kick.data.length) {
            return;
        }
        this.kick.data[index].c0 = c0;
        this.kick.data[index].c1 = c1;
        this.kick.data[index].c2 = c2;
        this.kick.data[index].hasData = true;
        this.markDirty();
    }

    public synchronized WrapperWeakCloneable<Pair<Boolean, Tuple<Double, Double, Double>>> getKickParam(int id, boolean isChip) {
        for (Kick.Data data : this.kick.data) {
            if (data.id == id && data.chip == isChip) {
                return new WrapperWeakCloneable<>(new Pair<>(data.hasData, new Tuple<>(data.c0, data.c1, data.c2)));
            }
        }
        return new WrapperWeakCloneable<>(new Pair<>(false, new Tuple<>(0.0, 1.0, 0.0)));
    }

    public synchronized double powerToSpeed(int power, int id, boolean isChip) {
        Tuple<Double, Double, Double> tuple = this.getKickParam(id, isChip).get().getSecond();
        double c0 = tuple.getFirst();
        double c1 = tuple.getSecond();
        double c2 = tuple.getThird();
        return c0 + c1 * power + c2 * power * power;
    }

    public synchronized boolean hasData(int id, boolean isChip) {
        return this.getKickParam(id, isChip).get().getFirst();
    }

    public synchronized int speedToPower(double speed, int id, boolean isChip) {
        if (speed == 0) {
            return 0;
        }
        Tuple<Double, Double, Double> tuple = this.getKickParam(id, isChip).get().getSecond();
        double c0 = tuple.getFirst();
        double c1 = tuple.getSecond();
        double c2 = tuple.getThird();
        if (FastMath.abs(c2) < MathHelper.DELTA) {
            if (FastMath.abs(c1) < MathHelper.DELTA) {
                return 0;
            }
            return (int)MathHelper.clamp(KICK_FACTOR * ((speed - c0) / c1), 0.0, 255.0);
        }
        double k = speed / c2 + c1 * c1 / (4.0 * c2 * c2) - c0 / c2;
        if (k < 0) {
            return (int)MathHelper.clamp(KICK_FACTOR * (-c1 / (2.0 * c2)), 0.0, 255.0);
        }
        if (c2 < 0) {
            return (int)MathHelper.clamp(KICK_FACTOR * (-c1 / (2.0 * c2) - FastMath.sqrt(k)), 0.0, 255.0);
        }
        return (int)MathHelper.clamp(KICK_FACTOR * (-c1 / (2.0 * c2) + FastMath.sqrt(k)), 0.0, 255.0);
    }

    public synchronized void markDirty() {
        this.isDirty = true;
    }

    public static void reset() {
        instance = null;
    }
}
