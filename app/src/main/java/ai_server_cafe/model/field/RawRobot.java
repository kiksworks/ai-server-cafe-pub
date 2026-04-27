package ai_server_cafe.model.field;

public class RawRobot {
    private final double x;
    private final double y;
    private final double theta;

    public RawRobot(double x, double y, double theta) {
        this.x = x;
        this.y = y;
        this.theta = theta;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getTheta() {
        return theta;
    }
}
