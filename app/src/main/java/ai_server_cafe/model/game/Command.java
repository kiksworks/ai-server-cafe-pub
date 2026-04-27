package ai_server_cafe.model.game;

import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.interfaces.AbstractCloneable;
import ai_server_cafe.util.interfaces.SerializableJson;
import ai_server_cafe.util.math.MathHelper;
import com.google.gson.JsonObject;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

/**
 * 制御用コマンド フィールド基準
 */
public class Command extends AbstractCloneable implements SerializableJson<Command> {
    protected Optional<Vector2D> targetPos;
    protected Vector2D targetVel;
    protected Optional<Double> targetTheta;
    protected double targetOmega;
    protected int dribble;
    protected Pair<EnumKickType, Integer> kickFlag;
    protected boolean halt;
    protected boolean sent;
    protected boolean isDirect;

    public Command() {
        this.targetPos = Optional.empty();
        this.targetVel = Vector2D.ZERO;
        this.targetTheta = Optional.empty();
        this.targetOmega = 0.0;
        this.dribble = 0;
        this.kickFlag = new Pair<>(EnumKickType.NONE, 0);
        this.halt = false;
        this.sent = false;
        this.isDirect = false;
    }

    public Command setTargetPosition(Vector2D targetPos) {
        this.targetPos = Optional.of(targetPos);
        return this;
    }

    public Command setTargetPosEmpty() {
        this.targetPos = Optional.empty();
        return this;
    }

    public Command setTargetVel(Vector2D targetVel) {
        this.targetVel = targetVel;
        return this;
    }

    public Command setTargetTheta(double theta) {
        this.targetTheta = Optional.of(theta);
        return this;
    }

    public Command setTargetThetaEmpty() {
        this.targetTheta = Optional.empty();
        return this;
    }

    public Command setTargetOmega(double omega) {
        this.targetOmega = omega;
        return this;
    }

    public Command setDribble(int dribble) {
        this.dribble = dribble;
        return this;
    }

    public Command setKickFlag(Pair<EnumKickType, Integer> kickFlag) {
        this.kickFlag = kickFlag;
        return this;
    }

    public Command setKickFlag(EnumKickType type, int value) {
        this.kickFlag = new Pair<>(type, value);
        return this;
    }

    public Command setDirect(boolean flag) {
        this.isDirect = flag;
        return this;
    }

    public Command setHalt(boolean halt) {
        this.halt = halt;
        return this;
    }

    synchronized public Command setSent(boolean sent) {
        this.sent = sent;
        return this;
    }

    synchronized public boolean wasSent() {
        return this.sent;
    }

    public Optional<Vector2D> getTargetPos() {
        return this.targetPos;
    }

    public Vector2D getTargetVel() {
        return this.targetVel;
    }

    public Optional<Double> getTargetTheta() {
        return this.targetTheta;
    }

    public double getTargetOmega() {
        return this.targetOmega;
    }

    public int getDribble() {
        return this.dribble;
    }

    public Pair<EnumKickType, Integer> getKickFlag() {
        return this.kickFlag;
    }

    public boolean isHalt() {
        return this.halt;
    }

    public boolean isDirect() {
        return this.isDirect;
    }

    @Override
    synchronized public Command clone() {
        Command command = new Command();
        if (this.targetPos.isPresent()) {
            command.setTargetPosition(new Vector2D(this.targetPos.get().getX(), this.targetPos.get().getY()));
        }
        if (this.targetTheta.isPresent()) {
            command.setTargetTheta(this.targetTheta.get());
        }
        command.setDribble(this.dribble);
        command.setTargetOmega(this.targetOmega);
        command.setKickFlag(this.kickFlag.getKey(), this.kickFlag.getValue());
        command.setTargetVel(new Vector2D(this.targetVel.getX(), this.targetVel.getY()));
        command.setHalt(this.halt);
        command.setSent(this.sent);
        command.setDirect(this.isDirect);
        return command;
    }

    public Command invert() {
        Command command = new Command();
        if (this.targetPos.isPresent()) {
            command.setTargetPosition(new Vector2D(-this.targetPos.get().getX(), -this.targetPos.get().getY()));
        }
        if (this.targetTheta.isPresent()) {
            command.setTargetTheta(MathHelper.wrapPI(this.targetTheta.get() + MathHelper.PI));
        }
        command.setDribble(this.dribble);
        command.setTargetOmega(this.targetOmega);
        command.setKickFlag(this.kickFlag.getKey(), this.kickFlag.getValue());
        command.setTargetVel(new Vector2D(-this.targetVel.getX(), -this.targetVel.getY()));
        command.setHalt(this.halt);
        command.setSent(this.sent);
        command.setDirect(this.isDirect);
        return command;
    }

    public String toString() {
        return "{targetPos:" + (this.targetPos.isEmpty() ? "null" : this.targetPos.toString()) + ",targetTheta:" + (this.targetTheta.isEmpty() ? "null" : this.targetTheta)
        + ",targetVel:" + this.targetVel + ",targetOmega:" + this.targetOmega + ",dribble:" + this.dribble + ",kickFlag:{" + this.kickFlag.getFirst() + "," + this.kickFlag.getSecond()
                + "},isHalt:" + this.halt + ",wasSent:" + this.sent + ",isDirect:" + this.isDirect + "}";
    }

    @Override
    public Command deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        Command command = new Command();
        if(!jsonObject.get("targetPos").isJsonNull() && !jsonObject.get("targetPos").getAsJsonObject().isEmpty()) {
            command.setTargetPosition(RecordData1.getVector2D(jsonObject.get("targetPos").getAsJsonObject()));
        }
        command.setTargetVel(RecordData1.getVector2D(jsonObject.get("targetVel").getAsJsonObject()));
        if(!jsonObject.get("targetTheta").isJsonNull() && !jsonObject.get("targetTheta").getAsJsonObject().isEmpty()) {
            command.setTargetTheta(jsonObject.get("targetTheta").getAsJsonObject().get("theta").getAsDouble());
        }
        command.setTargetOmega(jsonObject.get("targetOmega").getAsDouble());
        command.setDribble(jsonObject.get("dribble").getAsInt());
        final JsonObject kickFlagJO = jsonObject.get("kickFlag").getAsJsonObject();
        final EnumKickType kickType;
        switch (kickFlagJO.get("first").getAsString()) {
            case "CHIP" -> kickType = EnumKickType.CHIP;
            case "STRAIGHT" -> kickType = EnumKickType.STRAIGHT;
            default -> kickType = EnumKickType.NONE;
        }
        command.setKickFlag(kickType, kickFlagJO.get("second").getAsInt());
        command.setHalt(jsonObject.get("halt").getAsBoolean());
        command.setSent(jsonObject.get("sent").getAsBoolean());
        command.setDirect(jsonObject.get("isDirect").getAsBoolean());
        return command;
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        if(this.targetPos.isPresent()) {
            jsonObject.add("targetPos", RecordData1.convertVector2D(this.targetPos.get()));
        } else {
            jsonObject.add("targetPos", new JsonObject());
        }
        jsonObject.add("targetVel", RecordData1.convertVector2D(this.targetVel));
        if(this.targetTheta.isPresent()) {
            final JsonObject targetThetaJO = new JsonObject();
            targetThetaJO.addProperty("theta", this.targetTheta.get());
            jsonObject.add("targetTheta", targetThetaJO);
        } else {
            jsonObject.add("targetTheta", new JsonObject());
        }
        jsonObject.addProperty("targetOmega", this.targetOmega);
        jsonObject.addProperty("dribble", this.dribble);
        final JsonObject kickFlagJO = new JsonObject();
        kickFlagJO.addProperty("first", this.kickFlag.getFirst().toString());
        kickFlagJO.addProperty("second", this.kickFlag.getSecond());
        jsonObject.add("kickFlag", kickFlagJO);
        jsonObject.addProperty("halt", this.halt);
        jsonObject.addProperty("sent", this.sent);
        jsonObject.addProperty("isDirect", this.isDirect);
        return jsonObject;
    }
}
