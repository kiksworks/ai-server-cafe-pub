package ai_server_cafe.game.action;

import ai_server_cafe.model.game.Command;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.SerializableJson;
import com.google.gson.JsonObject;

import java.lang.reflect.InvocationTargetException;

public final class ActionWrapper extends AbstractAction implements SerializableJson<ActionWrapper> {
    private final String name;
    private final boolean finish;
    private final Command command;

    public ActionWrapper() {
        super(0, TeamColor.BLUE);
        this.finish = false;
        this.command = new Command();
        this.name = "";
    }

    public ActionWrapper(int id, TeamColor color, String name, boolean finish, Command command) {
        super(id, color);
        this.name = name;
        this.finish = finish;
        this.command = command;
    }

    @Override
    public Command execute() {
        return this.command;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isFinished() {
        return this.finish;
    }

    @Override
    public ActionWrapper deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        final int id = jsonObject.get("id").getAsInt();
        final TeamColor color = (jsonObject.get("color").getAsString().equals("YELLOW") ?
                TeamColor.YELLOW : TeamColor.BLUE);
        final String name = jsonObject.get("name").getAsString();
        final boolean finish = jsonObject.get("finish").getAsBoolean();
        final Command command = SerializableJson.deserializeJson(jsonObject.get("command").getAsJsonObject());
        return new ActionWrapper(id, color, name, finish, command);
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", this.id);
        jsonObject.addProperty("color", this.color.toString());
        jsonObject.addProperty("name", this.name);
        jsonObject.addProperty("finish", this.finish);
        jsonObject.add("command", SerializableJson.serializeJson(this.command));
        return jsonObject;
    }
}
