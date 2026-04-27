package ai_server_cafe.game.formation;

import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.game.role.RoleAttackWaiter;
import ai_server_cafe.game.role.RoleBallPlacer;
import ai_server_cafe.game.role.RoleChaser;
import ai_server_cafe.game.role.RoleDefense;
import ai_server_cafe.game.role.RoleExiter;
import ai_server_cafe.game.role.RoleHalt;
import ai_server_cafe.game.role.RoleKeeper;
import ai_server_cafe.game.role.RoleKicker;
import ai_server_cafe.game.role.RoleKickoffWaiter;
import ai_server_cafe.game.role.RolePenaltyKeeper;
import ai_server_cafe.game.role.RolePenaltyKicker;
import ai_server_cafe.game.role.RoleSupport;
import ai_server_cafe.game.role.RoleWaiter;
import ai_server_cafe.model.field.World;
import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.util.TeamColor;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractFormation {
    protected final TeamColor color;
    protected final int[] activeRobots;
    private boolean inverse;
    protected Optional<Vector2D> ballPlacePos;
    protected Optional<World> world;
    protected Optional<GcRefereeMessage.Referee.TeamInfo> teamInfo;
    protected boolean isOurBall;
    protected boolean isPrepare;
    protected long now;

    protected RoleAttackWaiter roleAttackWaiter;
    protected RoleChaser roleChaser;
    protected RoleDefense roleDefense;
    protected RoleHalt roleHalt;
    protected RoleKeeper roleKeeper;
    protected RoleWaiter roleWaiter;
    protected RoleKicker roleKicker;
    protected RoleKickoffWaiter roleKickoffWaiter;
    protected RoleBallPlacer roleBallPlacer;
    protected RoleExiter roleExiter;
    protected RoleSupport roleSupport;
    protected RolePenaltyKicker rolePenaltyKicker;
    protected RolePenaltyKeeper rolePenaltyKeeper;

    public AbstractFormation(TeamColor color, int[] ids) {
        this.ballPlacePos = Optional.of(Vector2D.ZERO);
        this.world = Optional.empty();
        this.teamInfo = Optional.empty();
        this.color = color;
        this.activeRobots = ids;
        this.inverse = false;
        this.isPrepare = false;
        this.isOurBall = false;
        this.now = 0L;

        this.roleAttackWaiter = new RoleAttackWaiter(color, ids);
        this.roleChaser = new RoleChaser(color, ids);
        this.roleDefense = new RoleDefense(color, ids);
        this.roleHalt = new RoleHalt(color, ids);
        this.roleKeeper = new RoleKeeper(color, ids);
        this.roleWaiter = new RoleWaiter(color, ids);
        this.roleKicker = new RoleKicker(color, ids);
        this.roleKickoffWaiter = new RoleKickoffWaiter(color, ids);
        this.roleBallPlacer = new RoleBallPlacer(color, ids);
        this.roleExiter = new RoleExiter(color, ids);
        this.roleSupport = new RoleSupport(color, ids);
        this.rolePenaltyKicker = new RolePenaltyKicker(color, ids);
        this.rolePenaltyKeeper = new RolePenaltyKeeper(color, ids);
    }

    public void setBallPlacePos(Vector2D vector2D) {
        this.ballPlacePos = Optional.of(vector2D);
    }

    public void setWorld(World world) {
        this.world = Optional.of(world);
    }

    public void setTeamInfo(Optional<GcRefereeMessage.Referee.TeamInfo> info) {
        this.teamInfo = info;
    }

    public void setInverse(boolean value) {
        this.inverse = value;
    }

    public void setOurBall(boolean value) {
        this.isOurBall = value;
    }

    public void setPrepare(boolean value) {
        this.isPrepare = value;
    }

    public List<? extends AbstractRole> update(long now) {
        this.now = now;
        List<? extends AbstractRole> result = this.execute();
        for (AbstractRole ar : result) {
            if (this.world.isPresent()) {
                ar.setWorld(this.world.get());
            }
            ar.setInverse(this.inverse);
        }
        return result;
    }

    protected abstract List<? extends AbstractRole> execute();

    public abstract String getName();

    /**
     * 処理が終わったかどうか
     * NORMAL_STARTなら強制的にFORCE_STARTになる
     * @return 処理終了かどうか
     */
    public abstract boolean isFinished();

    public static List<Integer> getVisibleIds(World world, TeamColor color, int[] activeRobots) {
        List<Integer> result = new ArrayList<>();
        for (int id : activeRobots) {
            if (world.getFriendlyRobotMap(color).containsKey(id)) {
                result.add(id);
            }
        }
        return result;
    }

    protected boolean isInverse() {
        return this.inverse;
    }
}
