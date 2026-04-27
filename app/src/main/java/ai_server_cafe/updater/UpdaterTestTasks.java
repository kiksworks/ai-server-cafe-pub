package ai_server_cafe.updater;

import ai_server_cafe.model.game.RobotTask;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.WrapperWeakCloneable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public final class UpdaterTestTasks {
    private static UpdaterTestTasks instance = null;
    private RobotTask[] blueTasks;
    private RobotTask[] yellowTasks;

    private UpdaterTestTasks() {
        this.blueTasks = new RobotTask[] {};
        this.yellowTasks = new RobotTask[] {};
    }

    public synchronized static UpdaterTestTasks getInstance() {
        if (instance == null) {
            instance = new UpdaterTestTasks();
        }
        return instance;
    }

    public synchronized void load(@Nonnull File file, @Nonnull TeamColor color) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            JsonReader reader = new JsonReader(new BufferedReader(new FileReader(file)));
            if (color.isYellow()) {
                this.yellowTasks = ((Tasks) gson.fromJson(reader, Tasks.class)).tasks;
            } else {
                this.blueTasks = ((Tasks) gson.fromJson(reader, Tasks.class)).tasks;
            }
            reader.close();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        if (this.yellowTasks == null) {
            this.yellowTasks = new RobotTask[] {};
        }
        if (this.blueTasks == null) {
            this.blueTasks = new RobotTask[] {};
        }
    }

    public synchronized WrapperWeakCloneable<RobotTask[]> getTasks(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            return new WrapperWeakCloneable<>(this.yellowTasks);
        } else {
            return new WrapperWeakCloneable<>(this.blueTasks);
        }
    }

    @ExcludedTest
    private static class Tasks {
        public RobotTask[] tasks;
    }
}
