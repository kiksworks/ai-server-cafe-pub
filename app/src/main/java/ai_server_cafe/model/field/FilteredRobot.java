package ai_server_cafe.model.field;

import ai_server_cafe.util.interfaces.IFuncParam2;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class FilteredRobot extends AbstractFilteredObject<RawRobot> {
    protected Optional<IFuncParam2<Optional<FilteredRobot>, FilteredRobot, Double>> estimator;

    public FilteredRobot() {
        super();
        this.estimator = Optional.empty();
    }

    @Override
    synchronized public FilteredRobot clone() {
        FilteredRobot fr = new FilteredRobot();
        fr.estimator = this.estimator;
        fr.lost = this.lost;
        fr.x = this.x;
        fr.y = this.y;
        fr.z = this.z;
        fr.vx = this.vx;
        fr.vy = this.vy;
        fr.vz = this.vz;
        fr.ax = this.ax;
        fr.ay = this.ay;
        fr.az = this.az;
        fr.jx = this.jx;
        fr.jy = this.jy;
        fr.jz = this.jz;
        fr.theta = this.theta;
        fr.omega = this.omega;
        fr.alpha = this.alpha;
        fr.zeta = this.zeta;
        return fr;
    }

    public FilteredRobot(@Nonnull RawRobot rawRobot) {
        this();
        this.x = rawRobot.getX();
        this.y = rawRobot.getY();
        this.theta = rawRobot.getTheta();
    }

    @Override
    synchronized public RawRobot getRaw() {
        return new RawRobot(this.x, this.y, this.theta);
    }


    synchronized public void setEstimator(IFuncParam2<Optional<FilteredRobot>, FilteredRobot, Double> estimator) {
        this.estimator = Optional.of(estimator);
    }

    synchronized public boolean hasEstimator() {
        return this.estimator.isPresent();
    }

    synchronized public Optional<IFuncParam2<Optional<FilteredRobot>, FilteredRobot, Double>> getEstimator() {
        return this.estimator;
    }

    synchronized public Optional<FilteredRobot> getStateAfter(double offset) {
        if (this.hasEstimator()) {
            return this.getEstimator().get().function(this, offset);
        }
        return Optional.empty();
    }

    @Override
    public FieldObject deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        FilteredRobot robot = new FilteredRobot();
        robot.setX(jsonObject.get("x").getAsDouble());
        robot.setX(jsonObject.get("x").getAsDouble());
        robot.setY(jsonObject.get("y").getAsDouble());
        robot.setZ(jsonObject.get("z").getAsDouble());
        robot.setVx(jsonObject.get("vx").getAsDouble());
        robot.setVy(jsonObject.get("vy").getAsDouble());
        robot.setVz(jsonObject.get("vz").getAsDouble());
        robot.setAx(jsonObject.get("ax").getAsDouble());
        robot.setAy(jsonObject.get("ay").getAsDouble());
        robot.setAz(jsonObject.get("az").getAsDouble());
        robot.setJx(jsonObject.get("jx").getAsDouble());
        robot.setJy(jsonObject.get("jy").getAsDouble());
        robot.setJz(jsonObject.get("jz").getAsDouble());
        robot.setTheta(jsonObject.get("theta").getAsDouble());
        robot.setOmega(jsonObject.get("omega").getAsDouble());
        robot.setAlpha(jsonObject.get("alpha").getAsDouble());
        return robot;
    }

    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("x", this.x);
        jsonObject.addProperty("y", this.y);
        jsonObject.addProperty("z", this.z);
        jsonObject.addProperty("vx", this.vx);
        jsonObject.addProperty("vy", this.vy);
        jsonObject.addProperty("vz", this.vz);
        jsonObject.addProperty("ax", this.ax);
        jsonObject.addProperty("ay", this.ay);
        jsonObject.addProperty("az", this.az);
        jsonObject.addProperty("jx", this.jx);
        jsonObject.addProperty("jy", this.jy);
        jsonObject.addProperty("jz", this.jz);
        jsonObject.addProperty("theta", this.theta);
        jsonObject.addProperty("omega", this.omega);
        jsonObject.addProperty("alpha", this.alpha);
        jsonObject.addProperty("zeta", this.zeta);
        return jsonObject;
    }
}
