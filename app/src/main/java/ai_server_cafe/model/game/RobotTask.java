package ai_server_cafe.model.game;

import ai_server_cafe.util.TeamColor;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.Pair;

import java.util.Optional;

public class RobotTask {
    public int id = 0;
    public TeamColor color = TeamColor.YELLOW;
    public String taskType = "action";
    public String taskName = "move";
    public boolean isFake = false;
    public TaskTarget target = new TaskTarget();

    public static class TaskTarget {
        String obj = "";
        double[] pos = null;
        double[] vel = new double[] {0.0, 0.0, 0.0};

        public boolean isBall() {
            return this.obj.equals("BALL");
        }

        public Optional<Pair<TeamColor, Integer>> getRobot() {
            try {
                if (this.obj.contains(TeamColor.BLUE.toString())) {
                    String s = this.obj.split(TeamColor.BLUE.toString())[1];
                    return Optional.of(new Pair<>(TeamColor.BLUE, Integer.valueOf(s)));
                }
                if (this.obj.contains(TeamColor.YELLOW.toString())) {
                    String s = this.obj.split(TeamColor.YELLOW.toString())[1];
                    return Optional.of(new Pair<>(TeamColor.YELLOW, Integer.valueOf(s)));
                }
            } catch (ArrayIndexOutOfBoundsException exception) {
                // DO NOTHING
            }
            return Optional.empty();
        }

        public Optional<Double> getDouble() {
            try {
                return Optional.of(Double.valueOf(this.obj));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        public Optional<Vector3D> getPos() {
            try {
                return Optional.of(new Vector3D(this.pos[0], this.pos[1], this.pos[2]));
            } catch (ArrayIndexOutOfBoundsException exception) {
                // DO NOTHING
            }
            return Optional.empty();
        }

        public Vector3D getVel() {
            try {
                return new Vector3D(this.vel[0], this.vel[1], this.vel[2]);
            } catch (ArrayIndexOutOfBoundsException exception) {
                // DO NOTHING
            }
            return Vector3D.ZERO;
        }
    }

    public boolean isTypeAction() {
        return this.taskType.equals("action");
    }

    public boolean isTypeRole() {
        return this.taskType.equals("role");
    }
}
