package ai_server_cafe.util.game;

import ai_server_cafe.util.math.Equation;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;

public class ControlHelper {

    /**
     * 台形制御の速度を返す(減速時)
     *
     * @param acc
     * @param r
     * @return
     */
    public static double getVelocityTrapezoidal(double acc, double r) {
        return FastMath.sqrt(2.0 * acc * FastMath.max(r, 0.0));
    }

    public static double getDistanceTrapezoidal(double acc, double vel) {
        return 0.5 * vel * vel / acc;
    }

    /**
     * 台形制御の速度を返す(減速時)
     *
     * @param acc
     * @param r
     * @param offsetVel
     * @return
     */
    public static double getVelocityTrapezoidal(double acc, double r, double offsetVel) {
        return FastMath.sqrt(2.0 * acc * FastMath.max(r, 0.0) + offsetVel * offsetVel);
    }

    public static double getVelocityExponential(double velMax, double accMax, double r) {
        // 時定数の逆数
        double k = (accMax * accMax) / (velMax * velMax);
        return FastMath.sqrt(k * FastMath.max(r * r, 0.0));
    }

    public static double getVelAngularTrapezoidal(double acc, double dTheta) {
        return FastMath.copySign(FastMath.sqrt(2.0 * acc * FastMath.max(FastMath.abs(dTheta), 0.0)), dTheta);
    }

    public static double getVelAngularExponential(double velMax, double accMax, double dTheta) {
        // 時定数の逆数
        double k = (accMax * accMax) / (velMax * velMax);
        return FastMath.copySign(FastMath.sqrt(k * FastMath.max(dTheta * dTheta, 0.0)), dTheta);
    }

    public static Vector2D criticalBraking(Vector2D rVec, Vector2D v0Vec,
                                           double deltaT, double vmax, double amax) {
        if (vmax <= 0.0 || amax <= 0.0 || deltaT <= 0.0) return Vector2D.ZERO;
        Vector2D nr = MathHelper.normalized(rVec);
        double r = rVec.getNorm() * 1.0; // 低速域カバー
        double v0 = v0Vec.dotProduct(nr);
        Pair<Double, Double> pair = makeAG(0.001 * r, 0.001 * v0, 0.001 * vmax, 0.001 * amax);
        if (pair.getValue() > 0.0) {
            double v = 1000.0 * (pair.getKey() * deltaT + 0.001 * v0) * FastMath.exp(-pair.getValue() * deltaT);
            return nr.scalarMultiply(v);
        } else {
            return nr.scalarMultiply(FastMath.min(vmax, v0 + amax * deltaT) * FastMath.min(rVec.getNorm(), 1.0));
        }
    }

    /**
     * @param v0     (v0 > 0.0)
     * @param deltaT ( = 1.0 / 60.0)
     */
    public static double fastestSpeedDecent(double v0, double accMax, double deltaT) {
        if (v0 <= 0.0 || accMax <= 0.0 || deltaT <= 0.0) return 0.0;
        return v0 * FastMath.exp(-accMax / v0 * deltaT);
    }

    /**
     * 臨界制動が効率よい最大距離
     *
     * @param v0   現在速度の進みたい方向成分 [v0 = vVec.dot(rVec.normalized())]
     * @param vmax 最大速度
     * @param amax 最大加速度
     */
    public static double cbEffectiveDistance(double v0, double vmax, double amax) {
        if (vmax <= 0.0 || amax <= 0.0) return 0.0;
        // 減速にしか使わないように (加速で使うと生成速度(=初動が小さい)的に障害物が超えられない,
        // withPlanner 改良必要)
        return 2.0 * v0 * v0 / amax;
    }

    public static double lowCover(double value, double c) {
        return FastMath.copySign(1.0, value) * FastMath.sqrt(c * c + value * value);
    }

    @Nonnull
    public static Vector2D lowCoverVec(@Nonnull Vector2D vel, double c) {
        return new Vector2D(lowCover(vel.getX(), c), lowCover(vel.getY(), c));
    }

    private static double criticalBrakingAngularBase(double abs_theta, double signedCurrent_omega,
                                                     double deltaT, double omegaMax,
                                                     double alphaMax) {
        Pair<Double, Double> pair = makeAG(abs_theta, signedCurrent_omega, omegaMax, alphaMax);
        if (omegaMax <= 0.0 || alphaMax <= 0.0 || deltaT <= 0.0) return 0.0;
        if (pair.getValue() > 0.0) {
            // v(t+Δt)
            return (pair.getKey() * deltaT + signedCurrent_omega) * FastMath.exp(-pair.getValue() * deltaT);
        } else {
            return FastMath.min(omegaMax, signedCurrent_omega + alphaMax / 60.0) * FastMath.min(abs_theta, 1.0);
        }
    }

    public static double criticalBrakingAngular(double target_theta, double current_theta,
                                                double current_omega, double deltaT,
                                                double omegaMax, double alphaMax) {
        if (omegaMax <= 0.0 || alphaMax <= 0.0 || deltaT <= 0.0) return 0.0;
        double theta = MathHelper.wrap2PI(target_theta) - MathHelper.wrap2PI(current_theta);
        double thetaPlus = theta;
        double thetaMinus = theta - MathHelper.TWO_PI;
        if (theta < 0.0) {
            thetaPlus = MathHelper.TWO_PI + theta;
            thetaMinus = theta;
        }

        double absThetaMax = FastMath.max(FastMath.abs(thetaPlus), FastMath.abs(thetaMinus));
        double comparedTime = 2.0 * FastMath.sqrt(absThetaMax / alphaMax);
        if (omegaMax * omegaMax / alphaMax < absThetaMax) {
            comparedTime = 2.0 * omegaMax / alphaMax +
                    (absThetaMax - (omegaMax * omegaMax / alphaMax)) / omegaMax;
        }

        double value =
                criticalBrakingAngularBase(thetaPlus, current_omega, deltaT, omegaMax, alphaMax);

        Pair<Double, Double> p = makeAG(thetaPlus, current_omega, omegaMax, alphaMax);
        Pair<Double, Double> n = makeAG(-thetaMinus, -current_omega, omegaMax, alphaMax);
        if (FastMath.abs(thetaPlus - distance(p.getKey(), p.getValue(), current_omega, comparedTime)) >
                FastMath.abs(-thetaMinus - distance(n.getKey(), n.getValue(), -current_omega, comparedTime))) {
            value =
                    -criticalBrakingAngularBase(-thetaMinus, -current_omega, deltaT, omegaMax, alphaMax);
        }
        return value;
    }

    public static double distance(double alpha, double gamma, double v0, double time) {
        return (alpha + gamma * v0) / (gamma * gamma) - ((gamma * alpha * time + gamma * v0 + alpha) *
                FastMath.exp(-gamma * time) / (gamma * gamma));
    }

    @Nonnull
    private static Pair<Double, Double> makeAG(double r0, double v0, double vmax,
                                               double amax) {
        double r = r0;
        // 0割り対策
        if (FastMath.abs(r) < 0.00000001) {
            r = 0.00000001;
        }
        boolean flag = true;
        double e = FastMath.exp(1.0);
        // 最大初加速度におけるγ
        double gamma = (v0 + FastMath.sqrt(v0 * v0 + r * amax)) / r;

        double valueV = Double.MAX_VALUE;
        double valueA = Double.MAX_VALUE;
        // 厳密には|alpha/gamma*exp(v0*gamma/alpha)|<=Vmax*eだがexpを2次近似
        if (FastMath.abs(v0) < 0.00000001) {
            // 0割り対策
            valueV = e * vmax / r;
        } else {
            double v = (vmax * e - v0) * (vmax * e - v0) - 2.0 * v0 * v0;
            if (v >= 0.0) {
                // 最大速度が速度制限を超えずに到達できるか？(初速度が速度制限を超えている場合(by
                // geogebra)=通常falseにならない)
                double xp =
                        ((vmax * e - v0) - FastMath.sqrt(v)) /
                                (v0 * v0);
                valueV = 1.0 / xp + v0;
            } else {
                flag = false;
            }
        }
        // 厳密には|alpha*exp(v0*gamma/alpha)|<=Amax*e*eだがexpを2次近似
        if (FastMath.abs(v0) < 0.00000001) {
            // 0割り対策
            valueA = FastMath.sqrt(e * e * r * amax) / r;
        } else {
            boolean flag0 = false;
            double root = Double.MAX_VALUE;
            for (double root3dim : Equation.realRoots3dim(
                    v0 * v0 * v0 * 0.5, 1.5 * v0 * v0 - amax * r * e * e, 2.0 * v0, 1.0)) {
                if (root3dim > 0) {
                    root = FastMath.min(root3dim, root);
                    flag0 = true;
                }
            }
            if (flag0) { // 最大減速度が減速度制限justで到達できるか？
                valueA = (1.0 / root + v0) / r;
            } else { // 変曲点なし == オーバーシュートせずに到達不可
                if (v0 * v0 - r * amax >= 0.0) {
                    // 減速度maxにてオーバーシュートして到達する場合のgamma候補と上のgamma候補を比較して小さい方を取る
                    valueA = FastMath.min(valueA, (v0 + FastMath.sqrt(v0 * v0 - r * amax)) / r);
                } else {
                    // 速度制限と初加速度制限に任せる
                }
            }
        }

        if (flag) {
            // 加速度制限、減速度制限、,速度制限によって得られたgammaの小さい方を取る
            gamma = FastMath.min(FastMath.min(valueV, valueA), gamma);
        }

        if (flag) {
            // 最終的なα
            double alpha = gamma * gamma * r - gamma * v0;
            // v(t+Δt)
            return new Pair<>(alpha, gamma);
        } else {
            return new Pair<>(0.0, -1.0);
        }
    }


    @Nonnull
    public static Pair<Double, Double> timeForTrapezoidalControl(double accel, double brake, double maxVel, double v0, double maxVe, double l) {
        double result = 0.0;
        if (v0 < 0.0) {
            l += 0.5 * v0 * v0 / brake;
            result += v0 / brake;
        }
        if (v0 <= maxVe && maxVe * maxVe - v0 * v0 > 2.0 * accel * l) {
            double ve = FastMath.sqrt(v0 * v0 + 2.0 * accel * l);
            result += (ve - v0) / accel;
            return new Pair<>(result, ve);
        }
        if (v0 > maxVe && v0 * v0 - maxVe * maxVe > 2.0 * brake * l) {
            double ve = FastMath.sqrt(v0 * v0 - 2.0 * brake * l);
            result += (v0 - ve) / brake;
            return new Pair<>(result, ve);
        }
        double vm = FastMath.sqrt((2.0 * accel * brake * l + brake * v0 * v0 + accel * maxVe * maxVe) / (accel + brake));
        if (vm > maxVel) {
            result += (maxVel - v0) / accel;
            l -= 0.5 * (maxVel * maxVel - v0 * v0) / accel;
            result += (maxVel - maxVe) / brake;
            l -= 0.5 * (maxVel * maxVel - maxVe * maxVe) / brake;
            result += l / maxVel;
            return new Pair<>(result, maxVe);
        }
        result += (vm - v0) / accel;
        result += (vm - maxVe) / brake;
        return new Pair<>(result, maxVe);
    }

    public static double timeForTrapezoidalControl(double accel, double brake, double maxVel, double l) {
        return timeForTrapezoidalControl(accel, brake, maxVel, 0, 0, l).getFirst();
    }
}
