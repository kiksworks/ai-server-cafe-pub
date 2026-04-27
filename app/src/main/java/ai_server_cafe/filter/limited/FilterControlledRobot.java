package ai_server_cafe.filter.limited;

import ai_server_cafe.filter.AbstractFilterObserver;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.RawRobot;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.game.FilterHelper;
import ai_server_cafe.util.interfaces.IFuncParam2;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class FilterControlledRobot extends AbstractFilterObserver<FilteredRobot, RawRobot> {
    // x, y, theta, vx, vy, vOmega
    private static final double[] SIGMAS = new double[]{1.6, 1.6, 1.6, 2.4, 2.4, 2.4};
    private static final double[] COEFFICIENTS = new double[]{3.0, 3.0, 3.0, 3.0, 3.0, 3.0};
    private final double lostDuration;
    private final Deque<Double> rawX;
    private final Deque<Double> rawY;
    private final Deque<Double> rawTheta;
    private final Deque<Double> rawVx;
    private final Deque<Double> rawVy;
    private final Deque<Double> rawOmega;
    private final double controlDelay;
    private final double visionDelay;
    private final LinkedHashMap<Vector3D, Double> inputCommands;
    private double prevRawX;
    private double prevRawY;
    private double prevRawTheta;
    // 前回の値
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") private Optional<FilteredRobot> robot;
    private double lastUpdateTime;
    private double appearTime;
    private double lastCommandUpdate;
    private double cycle;

    public FilterControlledRobot(double cycle, double delay, double lostDuration) {
        this.lostDuration = lostDuration;
        this.robot = Optional.empty();
        this.lastUpdateTime = 0.0;
        this.rawX = new ArrayDeque<>();
        this.rawY = new ArrayDeque<>();
        this.rawTheta = new ArrayDeque<>();
        this.rawVx = new ArrayDeque<>();
        this.rawVy = new ArrayDeque<>();
        this.rawOmega = new ArrayDeque<>();
        this.prevRawX = 0.0;
        this.prevRawY = 0.0;
        this.prevRawTheta = 0.0;
        this.controlDelay = delay;
        this.visionDelay = ConfigManager.getInstance().getConfig().filterConfig.visionDelay + SIGMAS[0] * COEFFICIENTS[0] * this.cycle;
        this.inputCommands = new LinkedHashMap<>();
        this.lastCommandUpdate = 0;
        this.cycle = cycle;
    }

    private static double gaussian(double t, double sigma) {
        return FastMath.exp(-(t * t) / (2.0 * sigma * sigma)) / FastMath.sqrt(2.0 * FastMath.PI * sigma * sigma);
    }

    @Nonnull
    public Optional<FilteredRobot> estimate(@Nonnull FilteredRobot robot, double t, LinkedHashMap<Vector3D, Double> map) {
        // 初期速度 [mm/s] = 現在値
        Vector2D v0 = robot.velocity();
        // 速度ゼロなら現在値を返す
        // (ゼロ除算の原因になるので)
        if (MathHelper.isDelta(v0.getNorm())) {
            if (FilterHelper.isOverOutSide(robot, ConfigManager.getInstance().getConfig())) {
                return Optional.empty();
            }
            return Optional.of(robot);
        }
        double now = TimeHelper.now();
        Vector3D inputCommand = Vector3D.ZERO;
        double lastControlTime = now - this.controlDelay;
        int count = 0;
        double x = robot.getX();
        double y = robot.getY();
        double theta = robot.getTheta();
        double vx = robot.getVx();
        double vy = robot.getVy();
        double omega = robot.getOmega();
        for (Map.Entry<Vector3D, Double> entry : map.entrySet()) {
            if (entry.getValue() < lastControlTime) {
                continue;
            } else if (entry.getValue() + this.controlDelay < now + t) {
                double tempDt = entry.getValue() - lastControlTime;
                if (count == 0) {
                    x += tempDt * vx;
                    y += tempDt * vy;
                    theta += tempDt * omega;
                } else {
                    omega = inputCommand.getZ();
                    theta += tempDt * omega;
                    // TODO use integral with omega
                    Vector3D globalVel = inputCommand;
                    vx = globalVel.getX();
                    vy = globalVel.getY();
                    x += tempDt * globalVel.getX();
                    y += tempDt * globalVel.getY();
                }
                inputCommand = entry.getKey();
                lastControlTime = entry.getValue();
            }
            count++;
        }
        if (lastControlTime + this.controlDelay < now + t) {
            // 惰性で進むと考える
            // TODO use integral with omega
            double additionalDt = now + t - (lastControlTime + this.controlDelay);
            x += vx * additionalDt;
            y += vy * additionalDt;
            theta += omega * additionalDt;
        }
        FilteredRobot result = new FilteredRobot();
        result.setX(x);
        result.setY(y);
        result.setTheta(theta);
        result.setVx(vx);
        result.setVy(vy);
        result.setOmega(omega);
        LinkedHashMap<Vector3D, Double> newMap = new LinkedHashMap<>(map);
        while (!newMap.isEmpty()) {
            if (now + t - newMap.firstEntry().getValue() > this.controlDelay) {
                newMap.pollFirstEntry();
            } else {
                break;
            }
        }
        result.setEstimator(new IFuncParam2<Optional<FilteredRobot>, FilteredRobot, Double>() {
            @Override
            public Optional<FilteredRobot> function(FilteredRobot robot, Double aDouble) {
                return estimate(robot, aDouble, newMap);
            }
        });
        if (FilterHelper.isOverOutSide(result, ConfigManager.getInstance().getConfig())) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    @Override
    public Optional<FilteredRobot> updateRaw(Optional<RawRobot> rawValue, double updateTime) {
        double dt = updateTime - this.lastUpdateTime;
        if (MathHelper.isDelta(FastMath.abs(dt))) {
            return this.robot;
        }

        this.lastUpdateTime = updateTime;

        if (rawValue.isPresent()) {
            double rawDt = updateTime - this.appearTime;
            double rawTheta = rawValue.get().getTheta();
            if (this.robot.isPresent()) {
                // 近い方の角度として足す この際+-180 degreesを超えることがある
                rawTheta = MathHelper.directionFrom(rawTheta, this.prevRawTheta) + this.prevRawTheta;
                if (FastMath.abs(rawTheta) > 1e6) {
                    // でかくなりすぎると計算誤差が大きくなるのでリセット
                    rawTheta = MathHelper.wrapPI(rawTheta);
                    this.prevRawTheta = MathHelper.wrapPI(this.prevRawTheta);
                    this.rawTheta.clear();
                }
            }
            double filterX =
                    this.getGaussianFilteredValue(rawValue.get().getX(), this.rawX, SIGMAS[0], COEFFICIENTS[0]);
            double filterY =
                    this.getGaussianFilteredValue(rawValue.get().getY(), this.rawY, SIGMAS[1], COEFFICIENTS[1]);
            double filterTheta = this.getGaussianFilteredValue(rawTheta, this.rawTheta, SIGMAS[2], COEFFICIENTS[2]);
            filterTheta = MathHelper.wrapPI(filterTheta);
            this.appearTime = updateTime;
            if (this.robot.isEmpty()) {
                FilteredRobot result = new FilteredRobot();
                result.setX(filterX);
                result.setY(filterY);
                result.setTheta(filterTheta);
                result.setEstimator(new IFuncParam2<Optional<FilteredRobot>, FilteredRobot, Double>() {
                    @Override
                    public Optional<FilteredRobot> function(FilteredRobot filteredRobot, Double aDouble) {
                        return Optional.of(filteredRobot);
                    }
                });
                this.prevRawX = rawValue.get().getX();
                this.prevRawY = rawValue.get().getY();
                this.prevRawTheta = rawValue.get().getTheta();
                this.robot = Optional.of(result);
                return this.robot;
            }

            double rawVx = (rawValue.get().getX() - this.prevRawX) / rawDt;
            double rawVy = (rawValue.get().getY() - this.prevRawY) / rawDt;
            double rawOmega = MathHelper.directionFrom(rawValue.get().getTheta(), this.prevRawTheta) / rawDt;
            double filterVx = this.getGaussianFilteredValue(rawVx, this.rawVx, SIGMAS[3], COEFFICIENTS[3]);
            double filterVy = this.getGaussianFilteredValue(rawVy, this.rawVy, SIGMAS[4], COEFFICIENTS[4]);
            double filterOmega = this.getGaussianFilteredValue(rawOmega, this.rawOmega, SIGMAS[5], COEFFICIENTS[5]);
            FilteredRobot filteredRobot = new FilteredRobot();
            filteredRobot.setX(filterX);
            filteredRobot.setY(filterY);
            filteredRobot.setTheta(filterTheta);
            filteredRobot.setVx(filterVx);
            filteredRobot.setVy(filterVy);
            filteredRobot.setOmega(filterOmega);

            double now = TimeHelper.now();
            double vx = filteredRobot.getVx();
            double vy = filteredRobot.getVy();
            double omega = filteredRobot.getOmega();
            double x = filteredRobot.getX();
            double y = filteredRobot.getY();
            double theta = filteredRobot.getTheta();
            double lastControlTime = now - this.visionDelay - this.controlDelay;
            int count = 0;
            Vector3D inputCommand = Vector3D.ZERO;
            for (Map.Entry<Vector3D, Double> entry : this.inputCommands.entrySet()) {
                if (entry.getValue() < lastControlTime) {
                    continue;
                } else if (now - lastControlTime > this.controlDelay) {
                    double tempDt = entry.getValue() - lastControlTime;
                    if (count == 0) {
                        x += tempDt * vx;
                        y += tempDt * vy;
                        theta += tempDt * omega;
                    } else {
                        omega = inputCommand.getZ();
                        theta += tempDt * omega;
                        // TODO use integral with omega
                        Vector3D globalVel = inputCommand;
                        vx = globalVel.getX();
                        vy = globalVel.getY();
                        x += tempDt * globalVel.getX();
                        y += tempDt * globalVel.getY();
                    }
                    if (now - entry.getValue() <= this.controlDelay) {
                        double additionalDt = now - this.controlDelay - lastControlTime;
                        if (additionalDt > 0) {
                            // TODO use integral with omega
                            omega = inputCommand.getZ();
                            theta += additionalDt * omega;
                            vx = inputCommand.getX();
                            vy = inputCommand.getY();
                            x += additionalDt * inputCommand.getX();
                            y += additionalDt * inputCommand.getY();
                        }
                    }
                    inputCommand = entry.getKey();
                    lastControlTime = entry.getValue();
                }
                count++;
            }
            FilteredRobot result = new FilteredRobot();
            result.setX(x);
            result.setY(y);
            result.setTheta(theta);
            result.setVx(vx);
            result.setVy(vy);
            result.setOmega(omega);
            LinkedHashMap<Vector3D, Double> map = new LinkedHashMap<>(this.inputCommands);
            while (!map.isEmpty()) {
                if (now - map.firstEntry().getValue() > this.controlDelay) {
                    map.pollFirstEntry();
                } else {
                    break;
                }
            }
            result.setEstimator(new IFuncParam2<Optional<FilteredRobot>, FilteredRobot, Double>() {
                @Override
                public Optional<FilteredRobot> function(FilteredRobot robot, Double aDouble) {
                    return estimate(robot, aDouble, map);
                }
            });
            this.robot = result.getStateAfter(this.controlDelay);
            this.prevRawX = rawValue.get().getX();
            this.prevRawY = rawValue.get().getY();
            this.prevRawTheta = rawTheta;
            return this.robot;
        } else {
            this.getGaussianFilteredValue(Double.NaN, this.rawX, SIGMAS[0], COEFFICIENTS[0]);
            this.getGaussianFilteredValue(Double.NaN, this.rawY, SIGMAS[1], COEFFICIENTS[1]);
            this.getGaussianFilteredValue(Double.NaN, this.rawTheta, SIGMAS[2], COEFFICIENTS[2]);
            this.getGaussianFilteredValue(Double.NaN, this.rawVx, SIGMAS[3], COEFFICIENTS[3]);
            this.getGaussianFilteredValue(Double.NaN, this.rawVy, SIGMAS[4], COEFFICIENTS[4]);
            this.getGaussianFilteredValue(Double.NaN, this.rawOmega, SIGMAS[5], COEFFICIENTS[5]);

            if (updateTime - this.appearTime > this.lostDuration) {
                this.rawX.clear();
                this.rawY.clear();
                this.rawTheta.clear();
                this.rawVx.clear();
                this.rawVy.clear();
                this.rawOmega.clear();
                this.robot = Optional.empty();
                return this.robot;
            } else if (this.robot.isPresent()) {
                this.robot = this.robot.get().getStateAfter(dt);
                return this.robot;
            }
        }

        return this.robot;
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

    @Override
    public void updateObserver(@Nonnull Object... args) {
        if (args.length != 3) throw new IllegalArgumentException("args.length=" + args.length + " is not 3");
        for (int i = 0; i < 3; i++) {
            if (!(args[i] instanceof Double))
                throw new IllegalArgumentException("args[" + i + "] is not an instance of Double");
        }
        double now = TimeHelper.now();
        double vx = (double) args[0];
        double vy = (double) args[1];
        double omega = (double) args[2];
        double dt = now - this.lastCommandUpdate;
        if (MathHelper.isDelta(dt)) {
            return;
        }
        this.lastCommandUpdate = now;
        this.inputCommands.putLast(new Vector3D(vx, vy, omega), now);
        while (true) {
            if (now - this.inputCommands.firstEntry().getValue() > this.visionDelay + this.controlDelay) {
                this.inputCommands.pollFirstEntry();
            } else {
                break;
            }
        }
    }
}
