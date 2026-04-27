package ai_server_cafe.game.action;

import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import java.util.Optional;

public class ActionGuard extends AbstractAction {
    private Optional<Vector2D> optPos;
    private Optional<Double> optTheta;

    public ActionGuard(int id, TeamColor color) {
        super(id, color);
        this.optPos = Optional.empty();
        this.optTheta = Optional.empty();
    }

    @Override
    protected Command execute() {
        final double acc = 2500;
        Command command = new Command();
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id))
            return command;

        final Field field = this.world.get().getField();
        final Vector2D robotPos = this.world.get().getFriendlyRobotMap(this.color).get(this.id).getRobot().position();
        final Vector2D ballPos = this.world.get().getBall().position();
        final Vector2D ballVel = this.world.get().getBall().velocity();

        if (this.optPos.isPresent()) {
            Vector2D targetPos = this.optPos.get();
            Vector2D toTargetVel = MathHelper.normalized(targetPos.subtract(robotPos)).scalarMultiply(FastMath.sqrt(2 * acc * MathHelper.distance2D(targetPos, robotPos)));

            Vector2D toGoalRotated =
                    MathHelper.applyRotation2D(MathHelper.normalized(field.getBackGoalCenter().subtract(ballPos)), 0.5 * FastMath.PI);
            // ボール速度の、ゴールとの垂直成分
            Vector2D ballFlow = toGoalRotated
                    .scalarMultiply(ballVel.dotProduct(toGoalRotated));
            command.setTargetVel(toTargetVel.add(ballFlow));

            // キックフラグ
            if (MathHelper.distance2D(ballPos, targetPos) < 2000
                    && MathHelper.inferiorAngle(ballPos.subtract(field.getBackGoalCenter()), new Vector2D(1, 0)) < 0.2 * FastMath.PI
                    && ballVel.getNorm() > 1000 && MathHelper.inferiorAngle(ballVel, targetPos.subtract(ballPos)) < 0.1 * FastMath.PI) {
                // ボールが近くて、ゴール正面方向にあって、ボールが向かってきているとき
                command.setKickFlag(new Pair<>(EnumKickType.CHIP, 255));
            }
        }
        if (this.optTheta.isPresent())
            command.setTargetTheta(this.optTheta.get());


        return command;
    }

    public void setPos(Vector2D pos) {
        this.optPos = Optional.of(pos);
    }

    public void setAngle(double theta) {
        this.optTheta = Optional.of(theta);
    }

    @Override
    public String getName() {
        return "guard";
    }

    @Override
    public boolean isFinished() {
        if (this.world.isEmpty() || !this.world.get().getFriendlyRobotMap(this.color).containsKey(this.id) || true)
            return false;
        IntegratedRobot iRobot = this.world.get().getFriendlyRobotMap(this.color).get(this.id);
        return (this.optPos.isEmpty() || this.optPos.get().subtract(iRobot.getRobot().position()).getNorm() < 5.0)
                && (this.optTheta.isEmpty() || FastMath.abs(this.optTheta.get() - iRobot.getRobot().getTheta()) < 0.1);
    }
}