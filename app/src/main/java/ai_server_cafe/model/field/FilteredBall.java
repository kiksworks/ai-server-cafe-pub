package ai_server_cafe.model.field;

import ai_server_cafe.util.interfaces.IFuncParam2;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class FilteredBall extends AbstractFilteredObject<RawBall> {
    protected Optional<IFuncParam2<Optional<FilteredBall>, FilteredBall, Double>> estimator;
    private boolean hasZ;
    private double cameraX;
    private double cameraY;
    private double cameraZ;

    public FilteredBall() {
        super();
        this.estimator = Optional.empty();
        this.hasZ = false;
        this.cameraX = 0.0;
        this.cameraY = 0.0;
        this.cameraZ = 0.0;
    }

    @Override
    synchronized public FilteredBall clone() {
        FilteredBall fb = new FilteredBall();
        fb.estimator = this.estimator;
        fb.hasZ = this.hasZ;
        fb.cameraX = this.cameraX;
        fb.cameraY = this.cameraY;
        fb.cameraZ = this.cameraZ;
        fb.lost = this.lost;
        fb.x = this.x;
        fb.y = this.y;
        fb.z = this.z;
        fb.vx = this.vx;
        fb.vy = this.vy;
        fb.vz = this.vz;
        fb.ax = this.ax;
        fb.ay = this.ay;
        fb.az = this.az;
        fb.jx = this.jx;
        fb.jy = this.jy;
        fb.jz = this.jz;
        fb.theta = this.theta;
        fb.omega = this.omega;
        fb.alpha = this.alpha;
        fb.zeta = this.zeta;
        return fb;
    }

    /**
     * @param rawBall
     */
    public FilteredBall(@Nonnull RawBall rawBall) {
        this();
        this.x = rawBall.getX();
        this.y = rawBall.getY();
        this.z = rawBall.getZ();
        this.hasZ = rawBall.hasZ();
        this.cameraX = rawBall.getCameraX();
        this.cameraY = rawBall.getCameraY();
        this.cameraZ = rawBall.getCameraZ();
    }

    @Override
    synchronized public RawBall getRaw() {
        return new RawBall(this.x, this.y, this.z, this.hasZ, this.cameraX, this.cameraY, this.cameraZ);
    }

    public void setHasZ(boolean value) {
        this.hasZ = value;
    }

    public void setCameraPos(double x, double y, double z) {
        this.cameraX = x;
        this.cameraY = y;
        this.cameraZ = z;
    }

    public void setEstimator(IFuncParam2<Optional<FilteredBall>, FilteredBall, Double> estimator) {
        this.estimator = Optional.of(estimator);
    }

    public boolean hasEstimator() {
        return this.estimator.isPresent();
    }

    public Optional<IFuncParam2<Optional<FilteredBall>, FilteredBall, Double>> getEstimator() {
        return this.estimator;
    }

    public Optional<FilteredBall> getStateAfter(double offset) {
        if (this.hasEstimator()) {
            return this.getEstimator().get().function(this, offset);
        }
        return Optional.empty();
    }

    @Override
    public FieldObject deserialize(JsonObject jsonObject)
            throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        FilteredBall ball = new FilteredBall();
        ball.setX(jsonObject.get("x").getAsDouble());
        ball.setY(jsonObject.get("y").getAsDouble());
        ball.setZ(jsonObject.get("z").getAsDouble());
        ball.setVx(jsonObject.get("vx").getAsDouble());
        ball.setVy(jsonObject.get("vy").getAsDouble());
        ball.setVz(jsonObject.get("vz").getAsDouble());
        ball.setAx(jsonObject.get("ax").getAsDouble());
        ball.setAy(jsonObject.get("ay").getAsDouble());
        ball.setAz(jsonObject.get("az").getAsDouble());
        ball.setJx(jsonObject.get("jx").getAsDouble());
        ball.setJy(jsonObject.get("jy").getAsDouble());
        ball.setJz(jsonObject.get("jz").getAsDouble());
        ball.setTheta(jsonObject.get("theta").getAsDouble());
        ball.setOmega(jsonObject.get("omega").getAsDouble());
        ball.setAlpha(jsonObject.get("alpha").getAsDouble());
        ball.setCameraPos(jsonObject.get("cameraX").getAsDouble(),
                jsonObject.get("cameraY").getAsDouble(),
                jsonObject.get("cameraZ").getAsDouble());
        ball.setHasZ(jsonObject.get("hasZ").getAsBoolean());
        ball.setLost(jsonObject.get("lost").getAsBoolean());
        return ball;
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
        jsonObject.addProperty("hasZ", this.hasZ);
        jsonObject.addProperty("cameraX", this.cameraX);
        jsonObject.addProperty("cameraY", this.cameraY);
        jsonObject.addProperty("cameraZ", this.cameraZ);
        jsonObject.addProperty("lost", this.lost);
        return jsonObject;
    }
}
