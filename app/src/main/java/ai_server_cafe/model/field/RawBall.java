package ai_server_cafe.model.field;

public class RawBall {
    private double x;
    private double y;
    private double z;
    private boolean hasZ;
    private double cameraZ;
    private double cameraY;
    private double cameraX;

    public RawBall(double x, double y, double z, boolean hasZ, double cameraX, double cameraY, double cameraZ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.hasZ = hasZ;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return this.z;
    }

    public double getCameraZ() {
        return this.cameraZ;
    }

    public double getCameraX() {
        return this.cameraX;
    }

    public double getCameraY() {
        return this.cameraY;
    }

    public boolean hasZ() {
        return this.hasZ;
    }
}