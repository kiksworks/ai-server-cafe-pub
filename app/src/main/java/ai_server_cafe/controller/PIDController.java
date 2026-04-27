package ai_server_cafe.controller;

import ai_server_cafe.config.Config;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.util.game.ControlHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;

public class PIDController extends AbstractController {
    protected final Config.PIDController config;
    protected final double cycle;
    protected Vector3D prevTargetVel;
    protected Vector2D prevErrorXY;
    protected double prevErrorOmega;
    protected double integralErrorNorm;
    protected double integralOmega;
    protected Deque<Double> prevTheta;

    public PIDController(double cycle, @Nonnull Config.PIDController config) {
        super(config.velocityMax);
        this.config = config;
        this.cycle = cycle;
        this.prevTargetVel = Vector3D.ZERO;
        this.prevErrorXY = Vector2D.ZERO;
        this.prevErrorOmega = 0.0;
        this.integralOmega = 0.0;
        this.integralErrorNorm = 0.0;
        this.prevTheta = new ArrayDeque<>();
    }

    private static double calcRatio(double before, double after) {
        // 比率の最大値である1.0を返す.
        if (before == 0.0) return 1.0;
        return after / before;
    }

    private static double gaussian(double t, double sigma) {
        return FastMath.exp(-(t * t) / (2.0 * sigma * sigma)) / FastMath.sqrt(2.0 * FastMath.PI * sigma * sigma);
    }

    @Override
    protected Vector3D updatePT(@Nonnull FilteredRobot robot, Field field, Vector2D position, double theta) {
        Vector2D targetVel = this.targetVelocity(position, robot.position(), robot.velocity());
        double targetOmega = this.targetVelAngular(theta, robot.getTheta(), robot.getOmega());
        return this.updateVO(robot, field, targetVel, targetOmega);
    }

    @Override
    protected Vector3D updateVT(@Nonnull FilteredRobot robot, Field field, Vector2D velocity, double theta) {
        double targetOmega = this.targetVelAngular(theta, robot.getTheta(), robot.getOmega());
        return this.updateVO(robot, field, velocity, targetOmega);
    }

    @Override
    protected Vector3D updatePO(@Nonnull FilteredRobot robot, Field field, Vector2D position, double omega) {
        Vector2D targetVel = this.targetVelocity(position, robot.position(), robot.velocity());
        return this.updateVO(robot, field, targetVel, omega);
    }

    /**
     *
     * @param robot
     * @param field
     * @param velocity
     * @param omega
     * @return robot based velocity and vel angular
     */
    @Override
    protected Vector3D updateVO(FilteredRobot robot, Field field, @Nonnull Vector2D velocity, double omega) {
        Vector3D targetVel = new Vector3D(velocity.getX(), velocity.getY(), omega);
        return this.getResultVel(
                this.getVelClampedWithField(this.getCalculatedVel(robot, this.getVelClampedWithAccel(targetVel)), field,
                        robot), robot);
    }

    public Vector3D getVelClampedWithAccel(Vector3D target) {
        Vector2D targetVel = MathHelper.getHead2(target);
        double targetOmega = target.getZ();
        Vector2D prevTargetXY = MathHelper.getHead2(this.prevTargetVel);
        double prevOmega = this.prevTargetVel.getZ();
        // brake
        Vector2D resultXY;
        Vector2D diffVel = targetVel.subtract(prevTargetXY);
        boolean isBack = targetVel.dotProduct(prevTargetXY) < 0.0;
        boolean isBrake = targetVel.dotProduct(diffVel) < 0.0;
        boolean isStraight = MathHelper.inferiorAngle(targetVel, prevTargetXY) < 0.15;
        double parallelDiffNorm = diffVel.dotProduct(MathHelper.normalized(prevTargetXY));
        Vector2D parallelDiff = MathHelper.normalized(prevTargetXY).scalarMultiply(parallelDiffNorm);
        Vector2D perpendicularDiff = diffVel.subtract(parallelDiff);
        double accel = isBrake ? this.config.brakeToTargetVelocity : this.config.accelToTargetVelocity;

        String key = "default";
        if (targetVel.subtract(prevTargetXY).getNorm() <= this.cycle * accel) {
            resultXY = targetVel;
            key = "normal";
        } else if (isBack) {
            Vector2D resultDiff = MathHelper.normalized(parallelDiff)
                    .scalarMultiply(FastMath.min(parallelDiff.getNorm(), this.cycle * accel));
            if (resultDiff.getNorm() < this.cycle * accel) {
                resultDiff = resultDiff.add(MathHelper.normalized(perpendicularDiff).scalarMultiply(
                        FastMath.min(perpendicularDiff.getNorm(),
                                FastMath.sqrt(this.cycle * this.cycle * accel * accel - resultDiff.getNormSq()))));
            }
            key = "back";
            resultXY = prevTargetXY.add(resultDiff);
        } else if (isBrake || isStraight) {
            key = "brake";
            resultXY = prevTargetXY.add(
                    MathHelper.normalized(diffVel).scalarMultiply(FastMath.min(diffVel.getNorm(), this.cycle * accel)));
        } else {
            Vector2D resultDiff = MathHelper.normalized(perpendicularDiff)
                    .scalarMultiply(FastMath.min(perpendicularDiff.getNorm(), this.cycle * accel));
            if (resultDiff.getNorm() < this.cycle * accel) {
                resultDiff = resultDiff.add(MathHelper.normalized(parallelDiff).scalarMultiply(
                        FastMath.min(parallelDiff.getNorm(),
                                FastMath.sqrt(this.cycle * this.cycle * accel * accel - resultDiff.getNormSq()))));
            }
            resultXY = prevTargetXY.add(resultDiff);
        }

        double accelAngular = this.config.accelToTargetVelAngular;
        double resultOmega = MathHelper.clamp(targetOmega, prevOmega - accelAngular * this.cycle,
                prevOmega + accelAngular * this.cycle);

        resultXY = MathHelper.normalized(resultXY)
                .scalarMultiply(FastMath.min(resultXY.getNorm(), this.config.velocityMax));
        resultOmega = MathHelper.clamp(resultOmega, -this.config.velAngularMax, this.config.velAngularMax);
        Vector3D result = new Vector3D(resultXY.getX(), resultXY.getY(), resultOmega);
        this.prevTargetVel = result;
        return result;
    }

    // pid, global coordinate
    public Vector3D getCalculatedVel(@Nonnull FilteredRobot robot, @Nonnull Vector3D target) {
        Vector2D currentVel = robot.velocity();
        double currentOmega = robot.getOmega();
        Vector2D targetVel = MathHelper.getHead2(target);
        double targetOmega = target.getZ();
        Vector2D errorXY = targetVel.subtract(currentVel);
        double errorOmega = targetOmega - currentOmega;
        this.integralOmega += FastMath.abs(errorOmega);
        this.integralErrorNorm += errorXY.getNorm();
        Vector2D derivativeErrorXY = errorXY.subtract(this.prevErrorXY);
        double derivativeErrorOmega = errorOmega - this.prevErrorOmega;
        this.prevErrorOmega = errorOmega;
        this.prevErrorXY = errorXY;
        Vector2D pidTargetVel = targetVel.add(errorXY.scalarMultiply(this.config.kp))
                .add(derivativeErrorXY.scalarMultiply(this.config.kd))
                .add(MathHelper.normalized(targetVel).scalarMultiply(this.integralErrorNorm * this.config.ki));
        double pidTargetOmega =
                targetOmega + errorOmega * this.config.kpAngle + derivativeErrorOmega * this.config.kdAngle +
                        FastMath.copySign(1.0, targetOmega) * this.integralOmega * this.config.kiAngle;
        return new Vector3D(pidTargetVel.getX(), pidTargetVel.getY(), pidTargetOmega);
    }

    // global coordinate
    public Vector3D getVelClampedWithField(@Nonnull Vector3D targetVel, @Nonnull Field field,
            @Nonnull FilteredRobot robot) {
        // 想定加速度
        double acc = this.config.accelToTargetVelocity;
        // フィールド外枠から出られる距離
        double margin = this.config.fieldMargin;
        // フィールド基準のロボット座標
        Vector2D robotPos = robot.position();
        // 移動可能範囲
        Vector2D maxP = new Vector2D(field.getMaxX() + margin, field.getMaxY() + margin);
        Vector2D minP = new Vector2D(field.getMinX() - margin, field.getMinY() - margin);
        // ロボット基準に変換
        Vector2D toMax = maxP.subtract(robotPos);
        Vector2D toMin = minP.subtract(robotPos);

        // 制限速度計算
        double vxMax = ControlHelper.getVelocityTrapezoidal(acc, toMax.getX());
        double vxMin = -ControlHelper.getVelocityTrapezoidal(acc, -toMin.getX());
        double vyMax = ControlHelper.getVelocityTrapezoidal(acc, toMax.getY());
        double vyMin = -ControlHelper.getVelocityTrapezoidal(acc, -toMin.getY());

        // 速度制限の比率
        double ratio = FastMath.min(calcRatio(targetVel.getX(), MathHelper.clamp(targetVel.getX(), vxMin, vxMax)),
                calcRatio(targetVel.getY(), MathHelper.clamp(targetVel.getY(), vyMin, vyMax)));

        return MathHelper.setHead2(targetVel, MathHelper.getHead2(targetVel.scalarMultiply(ratio)));
    }

    // robot based vel
    public Vector3D getResultVel(@Nonnull Vector3D targetVel, @Nonnull FilteredRobot robot) {
        Vector2D tempVel = MathHelper.getHead2(targetVel);
        Vector2D clampedVelWithRules =
                MathHelper.normalized(tempVel).scalarMultiply(FastMath.min(tempVel.getNorm(), this.velocityLimit));
        return MathHelper.setHead2(targetVel, MathHelper.applyRotation2D(clampedVelWithRules, -robot.getTheta()));
    }

    public Vector2D targetVelocity(@Nonnull Vector2D targetPosition, @Nonnull Vector2D currentPosition,
            @Nonnull Vector2D currentVelocity) {
        double brake = config.brakeToTargetPosition;
        Vector2D r = targetPosition.subtract(currentPosition);
        if (r.getNorm() < 50.0) {
            return ControlHelper.criticalBraking(r, currentVelocity, this.cycle, this.config.velocityMax,
                    this.config.brakeToTargetPosition);
        }
        return MathHelper.normalized((targetPosition.subtract(currentPosition))).scalarMultiply(
                ControlHelper.getVelocityTrapezoidal(brake, (targetPosition.subtract(currentPosition)).getNorm()));
    }

    public double targetVelAngular(double targetTheta, double currentTheta, double currentOmega) {
        //臨界制動は使わない
        ControlHelper.criticalBrakingAngular(targetTheta, currentTheta, currentOmega, this.cycle,
                this.config.velAngularMax, this.config.accelToTargetVelAngular);
        double dTheta = MathHelper.wrapPI(targetTheta - currentTheta);
        double brakeToTargetAngleSel = this.config.accelToTargetVelAngular;
        return FastMath.abs(dTheta) < 0.3 ?
                ControlHelper.getVelAngularExponential(this.config.velAngularMax, brakeToTargetAngleSel, dTheta) :
                ControlHelper.getVelAngularTrapezoidal(brakeToTargetAngleSel, dTheta);
    }

    private double getGaussianFilteredValue(double value, @Nonnull Deque<Double> prevValues, double sigma,
            double rangeCoefficient) {
        prevValues.add(value);
        int range = (int) (2.0 * rangeCoefficient * sigma);
        if (prevValues.size() > range) {
            prevValues.pollFirst();
        }
        if (!prevValues.isEmpty()) {
            double total = 0;
            double integralCoefficient = 0.0;
            for (int i = 0; i < prevValues.size(); i++) {
                double t = (i + 0.5 - 0.5 * prevValues.size());
                double prevValue = prevValues.stream().toList().get(i);
                if (Double.isNaN(prevValue)) {
                    continue;
                }
                double g = gaussian(t, sigma);
                total += g * prevValue;
                integralCoefficient += g;
            }
            if (Double.isNaN(value)) {
                return value;
            }
            total /= integralCoefficient;
            if (Double.isNaN(total)) {
                throw new MathArithmeticException(LocalizedFormats.ARITHMETIC_EXCEPTION, "value is NaN");
            }
            return total;
        }
        return value;
    }
}
