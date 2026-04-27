package ai_server_cafe.model.field;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionWrapper;
import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.RoleWrapper;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.AbstractCloneable;
import ai_server_cafe.util.interfaces.SerializableJson;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class IntegratedRobot extends AbstractCloneable implements SerializableJson<IntegratedRobot> {
    private final Optional<Command> command;
    private final Optional<AbstractAction> action;
    private final Optional<AbstractRole> role;
    private final int id;
    private final TeamColor color;
    private final FilteredRobot robot;

    public IntegratedRobot() {
        this.id = 0;
        this.color = TeamColor.YELLOW;
        this.action = Optional.empty();
        this.command = Optional.empty();
        this.role = Optional.empty();
        this.robot = new FilteredRobot();
    }

    public IntegratedRobot(int id, TeamColor color, @Nonnull FilteredRobot robot, Optional<Command> command, Optional<AbstractAction> action, Optional<AbstractRole> role) {
        this.id = id;
        this.color = color;
        this.command = command;
        this.action = action;
        this.role = role;
        this.robot = robot.clone();
    }

    public FilteredRobot getRobot() {
        return this.robot;
    }

    public Optional<AbstractAction> getAction() {
        return this.action;
    }

    public Optional<AbstractRole> getRole() {
        return this.role;
    }

    public Optional<Command> getCommand() {
        return this.command;
    }
    
    public int getId() {
    	return this.id;
    }
    
    public TeamColor getColor() {
    	return this.color;
    }

    @Override
    public IntegratedRobot clone() {
        return new IntegratedRobot(this.id, this.color, this.robot.clone(), this.command, this.action, this.role);
    }

    @Override
    public IntegratedRobot deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        if(!jsonObject.has("robot")) return null;
        final Object robotObj = SerializableJson.deserializeJson(jsonObject.get("robot").getAsJsonObject());
        if(robotObj instanceof FilteredRobot) {
            Optional<AbstractAction> action = Optional.empty();
            Optional<AbstractRole> role = Optional.empty();
            Optional<Command> command = Optional.empty();
            final int id = jsonObject.get("id").getAsInt();
            final TeamColor color = TeamColor.valueOf(jsonObject.get("color").getAsString());
            if(jsonObject.has("action")) {
                final Object actionObj = SerializableJson.deserializeJson(jsonObject.get("action").getAsJsonObject());
                if(actionObj instanceof ActionWrapper) {
                    action = Optional.of((ActionWrapper) actionObj);
                    command = Optional.of(((ActionWrapper) actionObj).execute());
                }
            }
            if(jsonObject.has("role")) {
                final Object roleObj = SerializableJson.deserializeJson(jsonObject.get("role").getAsJsonObject());
                if(roleObj instanceof RoleWrapper) role = Optional.of((RoleWrapper) roleObj);
            }
            return new IntegratedRobot(id, color, (FilteredRobot) robotObj, command, action, role);
        } else {
            return null;
        }
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", this.id);
        jsonObject.addProperty("color", this.color.toString());
        jsonObject.add("robot", SerializableJson.serializeJson(this.robot));
        if(this.role.isPresent()) {
            jsonObject.add("role", SerializableJson.serializeJson(
                    new RoleWrapper(this.color, new int[]{this.id},
                            this.role.get().getName(), this.role.get().isFinished(), this.role.get().getVisualizerTargets())));
        }
        if(this.command.isPresent()) {
            jsonObject.add("command", SerializableJson.serializeJson(this.command.get()));
            if(this.action.isPresent()) {
                jsonObject.add("action", SerializableJson.serializeJson(
                        new ActionWrapper(this.id, this.color, this.action.get().getName(),
                                this.action.get().isFinished(), this.command.get())));
            }
        }
        return jsonObject;
    }
}
