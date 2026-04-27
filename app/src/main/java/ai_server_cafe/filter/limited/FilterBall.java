package ai_server_cafe.filter.limited;

import ai_server_cafe.filter.AbstractFilterSame;
import ai_server_cafe.filter.PhysicalParameter;
import ai_server_cafe.filter.chip.ChipCalculator;
import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.model.field.RawBall;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.game.FilterHelper;
import ai_server_cafe.util.interfaces.IFuncParam2;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.exception.MathArithmeticException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class FilterBall extends AbstractFilterSame<FilteredBall, RawBall> {
    // x, y, z, vx, vy, vz
    private static final double[] SIGMAS = new double[]{1.6, 1.6, 1.6, 2.4, 2.4, 2.4};
    private static final double[] COEFFICIENTS = new double[]{3.0, 3.0, 3.0, 3.0, 3.0, 3.0};
    private final double lostDuration;
    private final Deque<Double> rawX;
    private final Deque<Double> rawY;
    private final Deque<Double> rawZ;
    private final Deque<Double> rawVx;
    private final Deque<Double> rawVy;
    private final Deque<Double> rawVz;
    private double prevRawX;
    private double prevRawY;
    private double prevRawZ;
    // 前回の値
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") private Optional<FilteredBall> ball;
    private double lastUpdateTime;
    private double appearTime;

    public FilterBall(double lostDuration) {
        this.lostDuration = lostDuration;
        this.ball = Optional.empty();
        this.lastUpdateTime = 0.0;
        this.rawX = new ArrayDeque<>();
        this.rawY = new ArrayDeque<>();
        this.rawZ = new ArrayDeque<>();
        this.rawVx = new ArrayDeque<>();
        this.rawVy = new ArrayDeque<>();
        this.rawVz = new ArrayDeque<>();
        this.prevRawX = 0.0;
        this.prevRawY = 0.0;
        this.prevRawZ = 0.0;
    }

    public static double calcDragCoefficient(double v) {
        // 特性長さ [m]
        final double l = 2.0 * PhysicalParameter.BALL_RADIUS;
        // レイノルズ数 Re
        final double re = (PhysicalParameter.AIR_DENSITY * l / PhysicalParameter.AIR_VISCOSITY) * (1.0e-3 * v);
        if (MathHelper.isEpsilon(re)) {
            return Double.POSITIVE_INFINITY;
        }
        // Clift and Grauvinの式の方がいいかも？
        // (参考: Morrison, Faith A. An Introduction to Fluid Mechanics . Cambridge University
        // Press, 2013.)
        return 24.0 / re + 2.6 * (re / 5.0) / (1.0 + FastMath.pow(re / 5.0, 1.52)) +
                0.411 * FastMath.pow(re / 2.63e5, -7.94) / (1 + FastMath.pow(re / 2.63e5, -8)) +
                0.25 * (re / 1.0e6) / (1.0 + re / 1.0e6);
    }

    @Nonnull
    public static Optional<FilteredBall> estimateChip(@Nonnull FilteredBall ball, double t) {
        // 初期速度 [mm/s] = 現在値
        Vector3D v0 = ball.velocity3D();
        // 速度ゼロなら現在値を返す
        // (ゼロ除算の原因になるので)
        if (MathHelper.isDelta(v0.getNorm())) {
            if (FilterHelper.isOverOutSide(ball, ConfigManager.getInstance().getConfig())) {
                return Optional.empty();
            }
            return Optional.of(ball);
        }

        // 抗力係数 Cd
        double cd = calcDragCoefficient(v0.getNorm());
        // 空気抵抗の係数
        if (cd == Double.POSITIVE_INFINITY) {
            if (FilterHelper.isOverOutSide(ball, ConfigManager.getInstance().getConfig())) {
                return Optional.empty();
            }
            return Optional.of(ball);
        }
        Pair<Pair<Double, Double>, Pair<Double, Double>> vrVz =
                new ChipCalculator(ball.velocity().getNorm() * 0.001, ball.getVz() * 0.001, 0.0, ball.getZ() * 0.001,
                        0.0001).getRZVrVz(t);
        Vector2D velXY = MathHelper.normalized(ball.velocity()).scalarMultiply(vrVz.getSecond().getFirst() * 1000.0);
        double velZ = vrVz.getSecond().getSecond() * 1000.0;
        Vector2D xy = MathHelper.normalized(ball.velocity()).scalarMultiply(vrVz.getFirst().getFirst() * 1000.0)
                .add(ball.position());
        double z = vrVz.getFirst().getSecond() * 1000.0;
        FilteredBall result = new FilteredBall();
        result.setX(xy.getX());
        result.setY(xy.getY());
        result.setZ(z);
        result.setVx(velXY.getX());
        result.setVy(velXY.getY());
        result.setVz(velZ);
        result.setEstimator(new IFuncParam2<Optional<FilteredBall>, FilteredBall, Double>() {
            @Override
            public Optional<FilteredBall> function(FilteredBall filteredBall, Double aDouble) {
                return filteredBall.getZ() > 0 ? estimateChip(filteredBall, aDouble) : estimate(filteredBall, aDouble);
            }
        });
        if (FilterHelper.isOverOutSide(result, ConfigManager.getInstance().getConfig())) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    @Nonnull
    public static Optional<FilteredBall> estimate(@Nonnull FilteredBall ball, double t) {
        // 初期速度 [mm/s] = 現在値
        Vector2D v0 = ball.velocity();
        // 速度ゼロなら現在値を返す
        // (ゼロ除算の原因になるので)
        if (MathHelper.isDelta(v0.getNorm())) {
            if (FilterHelper.isOverOutSide(ball, ConfigManager.getInstance().getConfig())) {
                return Optional.empty();
            }
            return Optional.of(ball);
        }

        // 抗力係数 Cd
        double cd = calcDragCoefficient(v0.getNorm());
        // 空気抵抗の係数
        if (cd == Double.POSITIVE_INFINITY) {
            if (FilterHelper.isOverOutSide(ball, ConfigManager.getInstance().getConfig())) {
                return Optional.empty();
            }
            return Optional.of(ball);
        }
        // 空気抵抗係数 [kg/m]
        double airCoeff = PhysicalParameter.AIR_RESISTANCE_COEFFICIENT;
        // 転がり抵抗による加速度 [m/s^2]
        double aFriction = PhysicalParameter.FRIC_COEF * PhysicalParameter.GRAVITY;
        double initV = v0.getNorm() * 0.001;
        double c = FastMath.atan(FastMath.sqrt(airCoeff / (PhysicalParameter.BALL_WEIGHT * aFriction)) * initV);
        double calcV = Math.max(FastMath.sqrt(PhysicalParameter.BALL_WEIGHT * aFriction / airCoeff) *
                FastMath.tan(c - FastMath.sqrt(airCoeff * aFriction / PhysicalParameter.BALL_WEIGHT) * t), 0.0) *
                1000.0;
        // 速度 [mm/s]
        Vector2D v = MathHelper.normalized(v0).scalarMultiply(calcV);
        double calcX = (PhysicalParameter.BALL_WEIGHT / airCoeff * FastMath.log(FastMath.abs(
                FastMath.cos(c - FastMath.sqrt(airCoeff * aFriction / PhysicalParameter.BALL_WEIGHT) * t))) -
                PhysicalParameter.BALL_WEIGHT / airCoeff * FastMath.log(FastMath.abs(FastMath.cos(c)))) * 1000.0;
        // 位置 [mm]
        Vector2D p = ball.position().add(MathHelper.normalized(v0).scalarMultiply(calcX));
        FilteredBall result = new FilteredBall();
        result.setX(p.getX());
        result.setY(p.getY());
        result.setVx(v.getX());
        result.setVy(v.getY());
        result.setEstimator(new IFuncParam2<Optional<FilteredBall>, FilteredBall, Double>() {
            @Override
            public Optional<FilteredBall> function(FilteredBall filteredBall, Double aDouble) {
                return filteredBall.getZ() > 0 ? estimateChip(filteredBall, aDouble) : estimate(filteredBall, aDouble);
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
    public Optional<FilteredBall> updateRaw(Optional<RawBall> rawValue, double updateTime) {
        double dt = updateTime - this.lastUpdateTime;
        if (MathHelper.isDelta(FastMath.abs(dt))) {
            return this.ball;
        }

        this.lastUpdateTime = updateTime;

        if (rawValue.isPresent()) {
            double rawDt = updateTime - this.appearTime;
            double filterX =
                    this.getGaussianFilteredValue(rawValue.get().getX(), this.rawX, SIGMAS[0], COEFFICIENTS[0]);
            double filterY =
                    this.getGaussianFilteredValue(rawValue.get().getY(), this.rawY, SIGMAS[1], COEFFICIENTS[1]);
            double filterZ =
                    this.getGaussianFilteredValue(rawValue.get().getZ(), this.rawZ, SIGMAS[2], COEFFICIENTS[2]);
            this.appearTime = updateTime;
            if (this.ball.isEmpty()) {
                FilteredBall result = new FilteredBall();
                result.setX(filterX);
                result.setY(filterY);
                result.setZ(filterZ);
                this.prevRawX = rawValue.get().getX();
                this.prevRawY = rawValue.get().getY();
                this.prevRawZ = rawValue.get().getZ();
                this.ball = Optional.of(result);
                return this.ball;
            }

            double rawVx = (rawValue.get().getX() - this.prevRawX) / rawDt;
            double rawVy = (rawValue.get().getY() - this.prevRawY) / rawDt;
            double rawVz = (rawValue.get().getZ() - this.prevRawZ) / rawDt;
            double filterVx = this.getGaussianFilteredValue(rawVx, this.rawVx, SIGMAS[3], COEFFICIENTS[3]);
            double filterVy = this.getGaussianFilteredValue(rawVy, this.rawVy, SIGMAS[4], COEFFICIENTS[4]);
            double filterVz = this.getGaussianFilteredValue(rawVz, this.rawVz, SIGMAS[5], COEFFICIENTS[5]);
            FilteredBall result = new FilteredBall();
            result.setX(filterX);
            result.setY(filterY);
            result.setZ(filterZ);
            result.setVx(filterVx);
            result.setVy(filterVy);
            result.setVz(filterVz);
            result.setEstimator(new IFuncParam2<Optional<FilteredBall>, FilteredBall, Double>() {
                @Override
                public Optional<FilteredBall> function(FilteredBall filteredBall, Double aDouble) {
                    return filteredBall.getZ() > 0 ? estimateChip(filteredBall, aDouble) :
                            estimate(filteredBall, aDouble);
                }
            });
            this.ball = result.getStateAfter(ConfigManager.getInstance().getConfig().filterConfig.visionDelay + SIGMAS[0] * COEFFICIENTS[0] * ConfigManager.getInstance().getCycleTime());

            this.prevRawX = rawValue.get().getX();
            this.prevRawY = rawValue.get().getY();
            this.prevRawZ = rawValue.get().getZ();
            return this.ball;
        } else {
            this.getGaussianFilteredValue(Double.NaN, this.rawX, SIGMAS[0], COEFFICIENTS[0]);
            this.getGaussianFilteredValue(Double.NaN, this.rawY, SIGMAS[1], COEFFICIENTS[1]);
            this.getGaussianFilteredValue(Double.NaN, this.rawZ, SIGMAS[2], COEFFICIENTS[2]);
            this.getGaussianFilteredValue(Double.NaN, this.rawVx, SIGMAS[3], COEFFICIENTS[3]);
            this.getGaussianFilteredValue(Double.NaN, this.rawVy, SIGMAS[4], COEFFICIENTS[4]);
            this.getGaussianFilteredValue(Double.NaN, this.rawVz, SIGMAS[5], COEFFICIENTS[5]);

            if (updateTime - this.appearTime > this.lostDuration) {
                this.rawX.clear();
                this.rawY.clear();
                this.rawZ.clear();
                this.rawVx.clear();
                this.rawVy.clear();
                this.rawVz.clear();
                this.ball = Optional.empty();
                return this.ball;
            } else if (this.ball.isPresent()) {
                this.ball = this.ball.get().getStateAfter(dt);
                return this.ball;
            }
        }

        return this.ball;
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
