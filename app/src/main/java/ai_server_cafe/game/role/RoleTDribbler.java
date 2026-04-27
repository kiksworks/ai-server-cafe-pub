package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionDribble;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RoleTDribbler extends AbstractRole {
    private Map<Integer, ActionDribble> dribbleMap;
    // 目標位置
    private Vector2D target;
    // 目標位置の中心
    private final Vector2D centerPos;
    // 中心からの距離
    private final double targetDist;
    // 乱数
    private final Random rand;

    public RoleTDribbler(TeamColor color, int[] ids) {
        super(color, ids);
        this.target = new Vector2D(500, 0);
        this.rand = new Random();
        this.dribbleMap = new HashMap<>();
        this.centerPos = new Vector2D(0, 0);
        this.targetDist = 1000.0;
        for (int id : ids) {
            dribbleMap.put(id, new ActionDribble(id, color));
        }
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        if (this.world.isEmpty() || this.roleIds.isEmpty()) {
            return list;
        }
        this.visualizerTargets.clear();

        int id = roleIds.getFirst();
        Vector2D ballPos = world.get().getBall().position();
        boolean isBack = ConfigManager.getInstance().getConfig().dribbleState.isBack;

        ActionDribble dribbler = this.dribbleMap.get(id);
        // 目標位置の更新
        if (MathHelper.distance2D(ballPos, this.target) < 100)
            this.target = this.centerPos.add(new Vector2D(rand.nextInt(200) - 100, rand.nextInt(200) - 100).scalarMultiply(3))
                    .add(MathHelper.getFromPolar(targetDist, MathHelper.direction(target, centerPos) + Math.PI));
        dribbler.setTarget(this.target);
        dribbler.setOffsetVel(0);
        dribbler.setBackDribble(isBack);
        visualizerTargets.add(this.target);
        list.add(dribbler);
        return list;
    }

    @Override
    public String getName() {
        return "t_dribbler";
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
