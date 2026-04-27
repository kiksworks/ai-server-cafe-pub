package ai_server_cafe.filter.chip;

import ai_server_cafe.filter.PhysicalParameter;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

/**
 * 斜方投射されたボールの挙動を4次のルンゲクッタを用いて予測するクラス
 */
public class ChipCalculator {
    private final double vr0;
    private final double vz0;
    private final double dt;
    private final double r0;
    private final double z0;

    /**
     * 円柱座標系での初期値を入力して予測用のインスタンスを作成
     * @param vr0 水平方向の初速度
     * @param vz0 鉛直方向の初速度
     * @param r0 水平方向の初期位置
     * @param z0 鉛直方向の初期位置
     * @param dt 予測に使う微小時間
     */
    public ChipCalculator(double vr0, double vz0, double r0, double z0, double dt) {
        this.vr0 = vr0;
        this.vz0 = vz0;
        this.dt = dt;
        this.r0 = r0;
        this.z0 = z0;
    }

    public static double fr(double t, double vr, double vz) {
        return -PhysicalParameter.AIR_RESISTANCE_COEFFICIENT * FastMath.sqrt(vr * vr + vz * vz) * vr / PhysicalParameter.BALL_WEIGHT;
    }

    public static double fz(double t, double vr, double vz) {
        return -PhysicalParameter.GRAVITY - PhysicalParameter.AIR_RESISTANCE_COEFFICIENT * FastMath.sqrt(vr * vr + vz * vz) * vz / PhysicalParameter.BALL_WEIGHT;
    }

    public static double fvz(double vz) {
        return vz;
    }

    public static double rv(double vr, double vz, double vr0, double vz0, double r0) {
        return r0 + (vr0 * vz0 - vr * vz) / PhysicalParameter.GRAVITY;
    }

    /**
     * t1秒後の予測値を返す
     * @param t1 予測する時間
     * @return [[r, z], [vr, vz]]の構造でt1秒後の予測値
     */
    public Pair<Pair<Double, Double>, Pair<Double, Double>> getRZVrVz(double t1) {
        int n = (int)(t1 / this.dt);
        double vr = this.vr0;
        double vz = this.vz0;
        double z = this.z0;
        double t = 0;
        for (int i = 0; i < n; i++) {
            double k0 = fr(t, vr, vz);
            double l0 = fz(t, vr, vz);
            double m0 = fvz(vz);
            double k1 = fr(t + 0.5 * dt, vr + 0.5 * k0 * dt, vz + 0.5 * l0 * dt);
            double l1 = fz(t + 0.5 * dt, vr + 0.5 * k0 * dt, vz + 0.5 * l0 * dt);
            double m1 = fvz(vz + 0.5 * l0 * dt);
            double k2 = fr(t + 0.5 * dt, vr + 0.5 * k1 * dt, vz + 0.5 * l1 * dt);
            double l2 = fz(t + 0.5 * dt, vr + 0.5 * k1 * dt, vz + 0.5 * l1 * dt);
            double m2 = fvz(vz + 0.5 * l1 * dt);
            double k3 = fr(t + dt, vr + k2 * dt, vz + l2 * dt);
            double l3 = fz(t + dt, vr + k2 * dt, vz + l2 * dt);
            double m3 = fvz(vz + l2 * dt);

            double k = dt / 6.0 * (k0 + 2 * k1 + 2 * k2 + k3);
            double l = dt / 6.0 * (l0 + 2 * l1 + 2 * l2 + l3);
            double m = dt / 6.0 * (m0 + 2 * m1 + 2 * m2 + m3);
            t += this.dt;
            vr += k;
            vz += l;
            z += m;
        }
        double r = rv(vr, vz, this.vr0, this.vz0, r0);
        return new Pair<>(new Pair<>(r, z), new Pair<>(vr, vz));
    }
}
