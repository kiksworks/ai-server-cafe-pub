package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.SerializableJson;
import ai_server_cafe.util.math.MathHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class RoleWrapper extends AbstractRole implements SerializableJson<RoleWrapper> {
    private final String name;
    private final boolean isFinished;

    public RoleWrapper() {
        super(TeamColor.BLUE, new int[] {});
        this.name = "";
        this.isFinished = false;
    }

    public RoleWrapper(TeamColor color, int[] ids, String name, boolean isFinished, List<Vector2D> targets) {
        super(color, ids);
        this.name = name;
        this.isFinished = isFinished;
        this.visualizerTargets = new ArrayList<>(targets);
    }

    @Override
    public List<? extends AbstractAction> execute() {
        return List.of();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isFinished() {
        return this.isFinished;
    }

    @Override
    public RoleWrapper deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        final TeamColor color = (jsonObject.get("color").getAsString().equals("YELLOW") ?
                TeamColor.YELLOW : TeamColor.BLUE);
        final List<Integer> idList = new ArrayList<>();
        for(JsonElement idJson : jsonObject.get("ids").getAsJsonArray()) idList.add(idJson.getAsInt());
        final String name = jsonObject.get("name").getAsString();
        final boolean isFinished = jsonObject.get("isFinished").getAsBoolean();
        List<Vector2D> targets = new ArrayList<>();
        for (JsonElement je : jsonObject.get("targets").getAsJsonArray()) {
            targets.add(RecordData1.getVector2D(je.getAsJsonObject()));
        }
        return new RoleWrapper(color, MathHelper.toArray(idList), name, isFinished, targets);
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("color", this.color.toString());
        JsonArray jsonArray = new JsonArray();
        for(int id : this.roleIds) jsonArray.add(id);
        jsonObject.add("ids", jsonArray);
        jsonObject.addProperty("name", this.name);
        jsonObject.addProperty("isFinished", this.isFinished);
        JsonArray jsonArray1 = new JsonArray();
        for (Vector2D vector2D : this.visualizerTargets) {
            jsonArray1.add(RecordData1.convertVector2D(vector2D));
        }
        jsonObject.add("targets", jsonArray1);
        return jsonObject;
    }
}
