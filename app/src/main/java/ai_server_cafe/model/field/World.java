package ai_server_cafe.model.field;

import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.AbstractCloneable;
import ai_server_cafe.util.interfaces.SerializableJson;
import ai_server_cafe.util.interfaces.WrapperCloneableMap;
import com.google.gson.JsonObject;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class World extends AbstractCloneable implements SerializableJson<World> {
    private final Map<Integer, IntegratedRobot> blueRobotMap;
    private final Map<Integer, IntegratedRobot> yellowRobotMap;
    private final FilteredBall ball;
    private final Field field;
    private final Vector2D blueDribbleStartPos;
    private final Vector2D yellowDribbleStartPos;
    private final boolean blueHaveBall;
    private final boolean yellowHaveBall;

    public World() {
        this.blueRobotMap = new HashMap<>();
        this.yellowRobotMap = new HashMap<>();
        this.ball = new FilteredBall();
        this.field = new Field();
        this.blueDribbleStartPos = Vector2D.ZERO;
        this.yellowDribbleStartPos = Vector2D.ZERO;
        this.blueHaveBall = false;
        this.yellowHaveBall = false;
    }

    public World(@Nonnull WrapperCloneableMap<Integer, IntegratedRobot> blueRobot, @Nonnull WrapperCloneableMap<Integer, IntegratedRobot> yellowRobot, FilteredBall ball, Field field, Vector2D blueDribbleStartPos, Vector2D yellowDribbleStartPos, boolean blueHaveBall, boolean yellowHaveBall) {
        this.blueRobotMap = blueRobot.get();
        this.yellowRobotMap = yellowRobot.get();
        this.ball = ball;
        this.field = field;
        this.blueDribbleStartPos = blueDribbleStartPos;
        this.yellowDribbleStartPos = yellowDribbleStartPos;
        this.blueHaveBall = blueHaveBall;
        this.yellowHaveBall = yellowHaveBall;
    }

    /**
     * DO NOT USE THIS! USE <code>UpdaterWorld.getWorld(true)</code> !
     * @return 反転したworld
     */
    public World getInverted() {
        Map<Integer, IntegratedRobot> blueInvertedMap = new HashMap<>();
        for (Map.Entry<Integer, IntegratedRobot> entry : this.blueRobotMap.entrySet()) {
            blueInvertedMap.put(entry.getKey(), new IntegratedRobot(entry.getValue().getId(), entry.getValue().getColor(),
                    (FilteredRobot) entry.getValue().getRobot().invert(), entry.getValue().getCommand().isPresent() ? Optional.of(entry.getValue().getCommand().get().invert())
                    : entry.getValue().getCommand(), entry.getValue().getAction(), entry.getValue().getRole()));
        }
        Map<Integer, IntegratedRobot> yellowInvertedMap = new HashMap<>();
        for (Map.Entry<Integer, IntegratedRobot> entry : this.yellowRobotMap.entrySet()) {
            yellowInvertedMap.put(entry.getKey(), new IntegratedRobot(entry.getValue().getId(), entry.getValue().getColor(),
                    (FilteredRobot) entry.getValue().getRobot().invert(), entry.getValue().getCommand().isPresent() ? Optional.of(entry.getValue().getCommand().get().invert())
                    : entry.getValue().getCommand(), entry.getValue().getAction(), entry.getValue().getRole()));
        }
        return new World(new WrapperCloneableMap<>(blueInvertedMap), new WrapperCloneableMap<>(yellowInvertedMap),
                (FilteredBall) this.ball.invert(), this.field.clone(), this.blueDribbleStartPos.negate(), this.yellowDribbleStartPos.negate(), this.blueHaveBall, this.yellowHaveBall);
    }

    @Override
    public World clone() {
        return new World(new WrapperCloneableMap<>(this.blueRobotMap), new WrapperCloneableMap<>(this.yellowRobotMap),
                this.ball.clone(), this.field.clone(), this.blueDribbleStartPos, this.yellowDribbleStartPos, this.blueHaveBall, this.yellowHaveBall);
    }

    public Map<Integer, IntegratedRobot> getFriendlyRobotMap(@Nonnull TeamColor color) {
        if (color.isYellow())
            return this.yellowRobotMap;
        return this.blueRobotMap;
    }

    public Map<Integer, IntegratedRobot> getOppositeRobotMap(@Nonnull TeamColor color) {
        if (color.isYellow())
            return this.blueRobotMap;
        return this.yellowRobotMap;
    }

    public FilteredBall getBall() {
        return this.ball;
    }

    public Field getField() {
        return this.field;
    }

    public Vector2D getDribbleStartPos(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            return this.yellowDribbleStartPos;
        } else {
            return this.blueDribbleStartPos;
        }
    }

    public boolean getHaveBall(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            return this.yellowHaveBall;
        } else {
            return this.blueHaveBall;
        }
    }

    @Override
    public World deserialize(@Nonnull JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Map<Integer, IntegratedRobot> blueRobotMap = new HashMap<>();
        final Map<Integer, IntegratedRobot> yellowRobotMap = new HashMap<>();
        final JsonObject yrJO = jsonObject.get("yellowRobotMap").getAsJsonObject();
        final JsonObject brJO = jsonObject.get("blueRobotMap").getAsJsonObject();
        for (int id = 0; id < ConfigManager.MAX_ROBOTS; id++) {
            if(yrJO.has(String.valueOf(id))) {
                yellowRobotMap.put(id, SerializableJson.deserializeJson(
                        yrJO.get(String.valueOf(id)).getAsJsonObject()));
            }
            if(brJO.has(String.valueOf(id))) {
                blueRobotMap.put(id, SerializableJson.deserializeJson(
                        brJO.get(String.valueOf(id)).getAsJsonObject()));
            }
        }
        final FilteredBall ball = SerializableJson.deserializeJson(jsonObject.get("ball").getAsJsonObject());
        final Field field = SerializableJson.deserializeJson(jsonObject.get("field").getAsJsonObject());
        final Vector2D blueDribbleStartPos = RecordData1.getVector2D(jsonObject.get("blueDribbleStartPos").getAsJsonObject());
        final Vector2D yellowDribbleStartPos = RecordData1.getVector2D(jsonObject.get("yellowDribbleStartPos").getAsJsonObject());
        return new World(new WrapperCloneableMap<>(blueRobotMap), new WrapperCloneableMap<>(yellowRobotMap), ball,
                field, blueDribbleStartPos, yellowDribbleStartPos,
                jsonObject.get("blueHaveBall").getAsBoolean(), jsonObject.get("yellowHaveBall").getAsBoolean());
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        JsonObject brJO = new JsonObject();
        JsonObject yrJO = new JsonObject();
        for(IntegratedRobot yr : this.yellowRobotMap.values()) {
            yrJO.add(String.valueOf(yr.getId()), SerializableJson.serializeJson(yr));
        }
        for(IntegratedRobot br : this.blueRobotMap.values()) {
            brJO.add(String.valueOf(br.getId()), SerializableJson.serializeJson(br));
        }
        jsonObject.add("blueRobotMap", brJO);
        jsonObject.add("yellowRobotMap", yrJO);
        jsonObject.add("ball", SerializableJson.serializeJson(this.ball));
        jsonObject.add("field", SerializableJson.serializeJson(this.field));
        jsonObject.add("blueDribbleStartPos", RecordData1.convertVector2D(this.blueDribbleStartPos));
        jsonObject.add("yellowDribbleStartPos", RecordData1.convertVector2D(this.yellowDribbleStartPos));
        jsonObject.addProperty("blueHaveBall", this.blueHaveBall);
        jsonObject.addProperty("yellowHaveBall", this.yellowHaveBall);
        return jsonObject;
    }
}
