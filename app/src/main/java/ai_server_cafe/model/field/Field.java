package ai_server_cafe.model.field;

import ai_server_cafe.util.interfaces.AbstractCloneable;
import ai_server_cafe.util.interfaces.SerializableJson;
import com.google.gson.JsonObject;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.lang.reflect.InvocationTargetException;

public class Field extends AbstractCloneable implements SerializableJson<Field> {
    private double width = 12000.0;
    private double height = 9000.0;
    private final double fieldMargin = 700.0;
    private final double gameMargin = 300.0;
    private double centerCircleRadius = 500.0;
    private double penaltyWidth = 3600.0;
    private double penaltyLength = 1800.0;
    private double goalLength = 200.0;
    private double goalWidth = 1800.0;

    public void setGameWidth(double value) {
        this.width = value;
    }

    public void setGameHeight(double value) {
        this.height = value;
    }

    public void setPenaltyWidth(double penaltyWidth) {
        this.penaltyWidth = penaltyWidth;
    }

    public void setPenaltyLength(double penaltyLength) {
        this.penaltyLength = penaltyLength;
    }

    public void setGoalLength(double goalLength) {
        this.goalLength = goalLength;
    }

    public void setGoalWidth(double goalWidth) {
        this.goalWidth = goalWidth;
    }

    public void setCenterCircleRadius(double radius) {
        this.centerCircleRadius = radius;
    }

    public double getGameWidth() {
        return this.width;
    }

    public double getGameHeight() {
        return this.height;
    }

    public double getCarpetWidth() {
        return this.width + 2.0 * fieldMargin;
    }

    public double getCarpetHeight() {
        return this.height + 2.0 * fieldMargin;
    }

    public double getFieldWidth() {
        return this.width + 2.0 * gameMargin;
    }

    public double getFieldHeight() {
        return this.height + 2.0 * gameMargin;
    }

    public double getCenterCircle() {
        return this.centerCircleRadius;
    }

    public double getPenaltyWidth() {
        return this.penaltyWidth;
    }

    public double getPenaltyLength() {
        return this.penaltyLength;
    }

    public double getGoalLength() {
        return this.goalLength;
    }

    public double getGoalWidth() {
        return this.goalWidth;
    }

    public double getMaxX() {
        return this.width / 2.0;
    }

    public double getMaxY() {
        return this.height / 2.0;
    }

    public double getMinX() {
        return -this.width / 2.0;
    }

    public double getMinY() {
        return -this.height / 2.0;
    }

    public Vector2D getFrontPenaltyMark() {
        // ペナルティマークの座標は
        //   - division Aのとき [-12.0 / 2.0 + 8.0, 0.0]
        //   - division Bのとき [ -9.0 / 2.0 + 6.0, 0.0]
        // なので，以下のようにして対応させる．
        return new Vector2D(this.getMinX() + 2.0 / 3.0 * this.getGameWidth(), 0.0);
    }

    public Vector2D getBackPenaltyMark() {
        // ペナルティマークの座標は
        //   - division Aのとき [12.0 / 2.0 - 8.0, 0.0]
        //   - division Bのとき [ 9.0 / 2.0 - 6.0, 0.0]
        // なので，以下のようにして対応させる．
        return new Vector2D(this.getMaxX() - 2.0 / 3.0 * this.getGameWidth(), 0.0);
    }

    public double getPenaltyFrontX() {
        return this.getMaxX() - getPenaltyLength();
    }

    public double getPenaltyBackX() {
        return this.getMinX() + getPenaltyLength();
    }

    public double getPenaltyMinY() {
        return -0.5 * getPenaltyWidth();
    }

    public double getPenaltyMaxY() {
        return 0.5 * getPenaltyWidth();
    }

    public double getGoalFrontX() {
        return this.getMaxX() + this.getGoalLength();
    }

    public double getGoalBackX() {
        return this.getMinX() - this.getGoalLength();
    }

    public double getGoalMinY() {
        return -0.5 * getGoalWidth();
    }

    public double getGoalMaxY() {
        return 0.5 * getGoalWidth();
    }

    public Vector2D getFrontGoalCenter() {
        return new Vector2D(this.getMaxX(), 0);
    }

    public Vector2D getFrontGoalRight() {
        return new Vector2D(this.getMaxX(), this.getGoalMinY());
    }

    public Vector2D getFrontGoalLeft() {
        return new Vector2D(this.getMaxX(), this.getGoalMaxY());
    }

    public Vector2D getBackGoalCenter() {
        return new Vector2D(this.getMinX(), 0);
    }

    public Vector2D getBackGoalRight() {
        return new Vector2D(this.getMinX(), this.getGoalMinY());
    }

    public Vector2D getBackGoalLeft() {
        return new Vector2D(this.getMinX(), this.getGoalMaxY());
    }

    public double getGameMargin() {
        return this.gameMargin;
    }

    @Override
    public Field clone() {
        Field copy = new Field();
        copy.width = this.width;
        copy.height = height;
        copy.centerCircleRadius = this.centerCircleRadius;
        copy.penaltyWidth = this.penaltyWidth;
        copy.penaltyLength = this.penaltyLength;
        copy.goalLength = this.goalLength;
        copy.goalWidth = this.goalWidth;
        return copy;
    }

    @Override
    public Field deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        Field field = new Field();
        field.setGameWidth(jsonObject.get("width").getAsDouble());
        field.setGameHeight(jsonObject.get("height").getAsDouble());
        field.setCenterCircleRadius(jsonObject.get("centerCircleRadius").getAsDouble());
        field.setPenaltyWidth(jsonObject.get("penaltyWidth").getAsDouble());
        field.setPenaltyLength(jsonObject.get("penaltyLength").getAsDouble());
        field.setGoalWidth(jsonObject.get("goalWidth").getAsDouble());
        field.setGoalLength(jsonObject.get("goalLength").getAsDouble());
        return field;
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("width", this.width);
        jsonObject.addProperty("height", this.height);
        jsonObject.addProperty("centerCircleRadius", this.centerCircleRadius);
        jsonObject.addProperty("penaltyWidth", this.penaltyWidth);
        jsonObject.addProperty("penaltyLength", this.penaltyLength);
        jsonObject.addProperty("goalLength", this.goalLength);
        jsonObject.addProperty("goalWidth", this.goalWidth);
        return jsonObject;
    }
}
