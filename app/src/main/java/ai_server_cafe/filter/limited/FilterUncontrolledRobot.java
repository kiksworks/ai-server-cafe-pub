package ai_server_cafe.filter.limited;

import ai_server_cafe.filter.AbstractFilterSame;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.RawRobot;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.game.FilterHelper;
import ai_server_cafe.util.interfaces.IFuncParam2;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class FilterUncontrolledRobot extends AbstractFilterSame<FilteredRobot, RawRobot> {
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
    private double prevRawX;
    private double prevRawY;
    private double prevRawTheta;
    // 前回の値
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") private Optional<FilteredRobot> robot;
    private double lastUpdateTime;
    private double appearTime;

    public FilterUncontrolledRobot(double lostDuration) {
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
    }

    @Nonnull
    public static Optional<FilteredRobot> estimate(@Nonnull FilteredRobot robot, double t) {
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

        FilteredRobot result = new FilteredRobot();
        result.setX(robot.getX() + robot.getVx() * t);
        result.setY(robot.getY() + robot.getVx() * t);
        result.setTheta(MathHelper.wrapPI(robot.getTheta() + robot.getOmega() * t));
        result.setVx(robot.getVx());
        result.setVy(robot.getVy());
        result.setOmega(robot.getOmega());
        result.setEstimator(new IFuncParam2<Optional<FilteredRobot>, FilteredRobot, Double>() {
            @Override
            public Optional<FilteredRobot> function(FilteredRobot robot, Double aDouble) {
                return estimate(robot, aDouble);
            }
        });
        if (FilterHelper.isOverOutSide(result, ConfigManager.getInstance().getConfig())) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    private static double gaussian(double t, double sigma) {
        return FastMath.exp(-(t * t) / (2.0 * sigma * sigma)) / FastMath.sqrt(2.0 * FastMath.PI * sigma * sigma);
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
            double filterTheta =
                    this.getGaussianFilteredValue(rawTheta, this.rawTheta, SIGMAS[2], COEFFICIENTS[2]);
            filterTheta = MathHelper.wrapPI(filterTheta);
            this.appearTime = updateTime;
            if (this.robot.isEmpty()) {
                FilteredRobot result = new FilteredRobot();
                result.setX(filterX);
                result.setY(filterY);
                result.setTheta(filterTheta);
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
            FilteredRobot result = new FilteredRobot();
            result.setX(filterX);
            result.setY(filterY);
            result.setTheta(filterTheta);
            result.setVx(filterVx);
            result.setVy(filterVy);
            result.setOmega(filterOmega);
            result.setEstimator(new IFuncParam2<Optional<FilteredRobot>, FilteredRobot, Double>() {
                @Override
                public Optional<FilteredRobot> function(FilteredRobot robot, Double aDouble) {
                    return estimate(robot, aDouble);
                }
            });
            this.robot = result.getStateAfter(ConfigManager.getInstance().getConfig().filterConfig.visionDelay + SIGMAS[0] * COEFFICIENTS[0] * ConfigManager.getInstance().getCycleTime());

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
}
