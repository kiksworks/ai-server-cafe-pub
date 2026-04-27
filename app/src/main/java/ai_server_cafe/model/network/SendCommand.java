package ai_server_cafe.model.network;

import ai_server_cafe.util.EnumKickType;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.AbstractCloneable;
import org.apache.commons.math3.util.Pair;


/**
 * 送信コマンド ロボット基準
 */
public class SendCommand extends AbstractCloneable {
    protected final int id;
    protected final TeamColor color;
    protected final Pair<EnumKickType, Integer> kickFlag;
    protected final int dribble;
    protected final double vx;
    protected final double vy;
    protected final double omega;
    protected final boolean direct;

    public SendCommand(int id, TeamColor color, Pair<EnumKickType, Integer> kickFlag, int dribble, double vx, double vy, double omega, boolean isDirect) {
        this.id = id;
        this.color = color;
        this.kickFlag = kickFlag;
        this.dribble = dribble;
        this.vx = vx;
        this.vy = vy;
        this.omega = omega;
        this.direct = isDirect;
    }

    public int getId() {
        return this.id;
    }

    public TeamColor getColor() {
        return this.color;
    }

    public double getVx() {
        return this.vx;
    }

    public double getVy() {
        return this.vy;
    }

    public double getOmega() {
        return this.omega;
    }

    public Pair<EnumKickType, Integer> getKickFlag() {
        return this.kickFlag;
    }

    public int getDribble() {
        return this.dribble;
    }

    public boolean isDirect() {
        return direct;
    }

    public String toString() {
        return this.color + "-" + this.id + " : [" + this.vx + ", " + this.vy + ", " + this.omega + "]";
    }

    @Override
    public SendCommand clone() {
        return new SendCommand(this.id, this.color, this.kickFlag, this.dribble, this.vx, this.vy, this.omega, this.direct);
    }
}
