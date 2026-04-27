package ai_server_cafe.records.ver1;

import ai_server_cafe.game.planner.path.PathSide;
import ai_server_cafe.game.strategy.XGType;
import ai_server_cafe.model.field.World;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.records.AbstractRecordData;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.SerializableJson;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GameのObstacleやWorldのデータなどを保存するクラス
 * @see SerializableJson
 */
public class RecordData1 extends AbstractRecordData {
    public JsonObject worldData = new JsonObject();
    public TeamData obstacleData = new TeamData();
    public TeamData pathPlannerData = new TeamData();
    public TeamData passTargetData = new TeamData();
    public TeamData xgData = new TeamData();
    public JsonObject refereeData = new JsonObject();

    public RecordData1() {
        this.format = 1;
    }

    public static class TeamData {
        public JsonObject blue = new JsonObject();
        public JsonObject yellow = new JsonObject();
    }

    @Override
    public boolean matchFormat() {
        return (this.format == 1);
    }

    public World getWorld() {
        return SerializableJson.deserializeJson(this.worldData);
    }

    public void setObstacleData(@Nonnull TeamColor color, int id, @Nonnull List<AbstractObstacle> obstacles) {
        if (color.isYellow()) {
            this.obstacleData.yellow.add(String.valueOf(id), convertList(obstacles));
        } else {
            this.obstacleData.blue.add(String.valueOf(id), convertList(obstacles));
        }
    }

    public List<AbstractObstacle> getObstacles(@Nonnull TeamColor color, int id) {
        if (color.isYellow()) {
            return getList(this.obstacleData.yellow.getAsJsonArray(String.valueOf(id)));
        } else {
            return getList(this.obstacleData.blue.getAsJsonArray(String.valueOf(id)));
        }
    }

    @Nonnull
    public static <T extends SerializableJson<T>> JsonArray convertList(@Nonnull List<T> serializableJsons) {
        JsonArray result = new JsonArray();
        for (T serializableJson : serializableJsons) {
            result.add(SerializableJson.serializeJson(serializableJson));
        }
        return result;
    }

    @Nonnull
    public static JsonObject convertVector2D(Vector2D vector2D) {
        JsonObject jo = new JsonObject();
        jo.addProperty("x", vector2D.getX());
        jo.addProperty("y", vector2D.getY());
        return jo;
    }

    @Nonnull
    public static Vector2D getVector2D(@Nonnull JsonObject jsonObject) {
        return new Vector2D(jsonObject.get("x").getAsDouble(), jsonObject.get("y").getAsDouble());
    }

    /**
     *
     * @return 引数のarrayを指定したクラスTに変換する
     * @param <T> SerializableJsonを継承したクラス
     */
    @Nonnull
    public static <T extends SerializableJson<T>> List<T> getList(@Nonnull JsonArray array) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            result.add(SerializableJson.deserializeJson(array.get(i).getAsJsonObject()));
        }
        return result;
    }

    @Nonnull
    public static <T> List<T> getListFromClass(@Nonnull JsonArray array, Class<? extends T> clazz) {
        List<T> result = new ArrayList<>();
        Gson gson = new Gson();
        for (int i = 0; i < array.size(); i++) {
            result.add(gson.fromJson(array.get(i).getAsJsonObject(), clazz));
        }
        return result;
    }

    /**
     *
     * @implNote 上記のconvertListと違い，TにSerializableJsonが継承されている必要がない．
     * @param list クラスのList
     * @return Listの変数をJsonArrayに変換する
     */
    @Nonnull
    public static <T> JsonArray convertListFromClass(@Nonnull List<T> list) {
        JsonArray result = new JsonArray();
        Gson gson = new Gson();
        for (T item : list) {
            result.add(gson.toJsonTree(item));
        }
        return result;
    }

    public void setRefereeData(@Nonnull List<GcRefereeMessage.Referee> referees) {
        JsonArray result = new JsonArray();
        for (GcRefereeMessage.Referee referee : referees) {
            JsonArray array = new JsonArray();
            for (byte b : referee.toByteArray()) {
                array.add(b);
            }
            result.add(array);
        }
        this.refereeData.add("refereeList", result);
    }

    public List<GcRefereeMessage.Referee> getRefereeData() {
        List<GcRefereeMessage.Referee> result = new ArrayList<>();
        JsonArray array = this.refereeData.getAsJsonArray("refereeList");
        for (int i = 0; i < array.size(); i++) {
            JsonArray byteArray = array.get(i).getAsJsonArray();
            byte[] bytes = new byte[byteArray.size()];
            for (int j = 0; j < byteArray.size(); j++) {
                bytes[j] = byteArray.get(j).getAsByte();
            }
            try {
                result.add(GcRefereeMessage.Referee.parseFrom(bytes));
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    /* ========================= PathPlanner =========================*/

    public void setPathPlannerData(@Nonnull TeamColor color, @Nonnull PathPlannerWrapper planner, int id) {
        final JsonObject plannerObj = new JsonObject();
        plannerObj.add("pathSide", RecordData1.convertListFromClass(planner.getPathSide()));
        plannerObj.addProperty("hasResult", planner.getResult());
        plannerObj.addProperty("step", planner.getStep());
        if(color.isYellow()) {
            this.pathPlannerData.yellow.add(String.valueOf(id), plannerObj);
        } else {
            this.pathPlannerData.blue.add(String.valueOf(id), plannerObj);
        }
    }

    public PathPlannerWrapper getPlannerData(@Nonnull TeamColor color, int id) {
        if(this.pathPlannerData.yellow.has(String.valueOf(id))
                || this.pathPlannerData.blue.has(String.valueOf(id))) {
            final JsonObject plannerObj = (color.isYellow() ?
                    this.pathPlannerData.yellow.get(String.valueOf(id)).getAsJsonObject()
                    : this.pathPlannerData.blue.get(String.valueOf(id)).getAsJsonObject());
            final List<PathSide> pathSide = RecordData1.getListFromClass(
                    plannerObj.get("pathSide").getAsJsonArray(), PathSide.class);
            final double step = plannerObj.get("step").getAsDouble();
            final boolean hasResult = plannerObj.get("hasResult").getAsBoolean();
            return new PathPlannerWrapper(pathSide, step, hasResult);
        }
        return null;
    }

    /* ========================= PassScore =========================*/

    public void setPassScoreData(@Nonnull TeamColor color, @Nonnull PassTargetWrapper passTarget) {
        final Map<Vector2D, Double> passScore = passTarget.getScoreMap();
        JsonArray passScoreMapArray = new JsonArray();
        for (Vector2D target : passScore.keySet()) {
            JsonObject mapObj = new JsonObject();
            mapObj.add("key", convertVector2D(target));
            mapObj.addProperty("value", passScore.get(target));
            passScoreMapArray.add(mapObj);
        }

        final List<Vector2D> passTargets = passTarget.getPassTargets();
        JsonArray passTargetListArray = new JsonArray();
        for (Vector2D target : passTargets) {
            passTargetListArray.add(convertVector2D(target));
        }

        final Vector2D kickerPos = passTarget.getKickerPos();
        if(color.isYellow()) {
            this.passTargetData.yellow.add("passScore", passScoreMapArray);
            this.passTargetData.yellow.add("passTargets", passTargetListArray);
            this.passTargetData.yellow.add("kickerPos", convertVector2D(kickerPos));
        } else {
            this.passTargetData.blue.add("passScore", passScoreMapArray);
            this.passTargetData.blue.add("passTargets", passTargetListArray);
            this.passTargetData.blue.add("kickerPos", convertVector2D(kickerPos));
        }
    }

    public PassTargetWrapper getPassTargetData(@Nonnull TeamColor color) {
        final Map<Vector2D, Double> passScore = new HashMap<>();
        final JsonObject passTargetObj = (color.isYellow() ?
                this.passTargetData.yellow : this.passTargetData.blue);
        for (JsonElement je : passTargetObj.get("passScore").getAsJsonArray()) {
            final JsonObject passScoreMapObj = je.getAsJsonObject();
            final JsonObject vectorObj = passScoreMapObj.get("key").getAsJsonObject();
            final Vector2D keyVector = getVector2D(vectorObj);
            passScore.put(keyVector, passScoreMapObj.get("value").getAsDouble());
        }

        final List<Vector2D> passTargets = new ArrayList<>();
        for (JsonElement je : passTargetObj.get("passTargets").getAsJsonArray()) {
            final JsonObject passTargetsListObj = je.getAsJsonObject();
            passTargets.add(getVector2D(passTargetsListObj));
        }

        final JsonObject kickerObj = passTargetObj.get("kickerPos").getAsJsonObject();
        final Vector2D kickerPos = getVector2D(kickerObj);

        return new PassTargetWrapper(passScore, passTargets, kickerPos);
    }

    /* ========================= XG =========================*/

    public void setXGData(@Nonnull TeamColor color, StrategyWrapper strategy) {
        JsonObject xgMapObj = new JsonObject();
        xgMapObj.add("TYPEA", this.convertFromXGMap(strategy.getXGMap(XGType.TYPEA)));
        xgMapObj.add("TYPEB", this.convertFromXGMap(strategy.getXGMap(XGType.TYPEB)));
        xgMapObj.add("TYPEC", this.convertFromXGMap(strategy.getXGMap(XGType.TYPEC)));
        if(color.isYellow()) {
            this.xgData.yellow.add("xgMap", xgMapObj);
            this.xgData.yellow.addProperty("attackSide", strategy.getAttackSide());
        } else {
            this.xgData.blue.add("xgMap", xgMapObj);
            this.xgData.blue.addProperty("attackSide", strategy.getAttackSide());
        }
    }

    private JsonArray convertFromXGMap(Map<Vector2D, Double> xgMap) {
        JsonArray xgMapArray = new JsonArray();
        for(Vector2D target : xgMap.keySet()) {
            JsonObject mapObj = new JsonObject();
            mapObj.add("key", convertVector2D(target));
            mapObj.addProperty("value", xgMap.get(target));
            xgMapArray.add(mapObj);
        }
        return xgMapArray;
    }

    public StrategyWrapper getXGData(@Nonnull TeamColor color) {
        final JsonObject xgDataObj = (color.isYellow() ? this.xgData.yellow : this.xgData.blue);
        final int attackSide = xgDataObj.get("attackSide").getAsInt();
        final JsonObject xgMapObj = xgDataObj.get("xgMap").getAsJsonObject();
        final Map<Vector2D, Double> xgMapA = this.convertFromXGArray(xgMapObj.get("TYPEA").getAsJsonArray());
        final Map<Vector2D, Double> xgMapB = this.convertFromXGArray(xgMapObj.get("TYPEB").getAsJsonArray());
        final Map<Vector2D, Double> xgMapC = this.convertFromXGArray(xgMapObj.get("TYPEC").getAsJsonArray());
        return new StrategyWrapper(attackSide, xgMapA, xgMapB, xgMapC);
    }

    private Map<Vector2D, Double> convertFromXGArray(JsonArray xgMapArray) {
        final Map<Vector2D, Double> xgMap = new HashMap<>();
        for(JsonElement je : xgMapArray) {
            final JsonObject vectorObj = je.getAsJsonObject().get("key").getAsJsonObject();
            xgMap.put(getVector2D(vectorObj), je.getAsJsonObject().get("value").getAsDouble());
        }
        return xgMap;
    }
}
