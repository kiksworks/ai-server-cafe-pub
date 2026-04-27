package ai_server_cafe.game.role;

import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.WithPlanner;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.obstacle.CommonObstacles;
import ai_server_cafe.model.field.obstacle.interfaces.AbstractObstacle;
import ai_server_cafe.model.network.OptionalCommand;
import ai_server_cafe.updater.UpdaterOptionalCommand;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoleLocate extends AbstractRole {
    private boolean commandInverse = false;

    public RoleLocate(TeamColor color, int[] ids) {
        super(color, ids);
    }

    @Override
    protected List<? extends AbstractAction> execute() {
        List<AbstractAction> list = new ArrayList<>();
        boolean isRemove = true;
        if (this.world.isEmpty() || roleIds.isEmpty()) {
            return list;
        }
        List<Integer> tmpIds = new ArrayList<>(roleIds);
        if(!UpdaterOptionalCommand.getInstance().getCommands().get().isEmpty()) {
            OptionalCommand oc = UpdaterOptionalCommand.getInstance().getCommands().get().getFirst();
            for (Map.Entry<Integer, Vector2D> entry : oc.getRobotPos().getMap(this.color).entrySet()) {
                int id = entry.getKey();
                Vector2D targetPos = entry.getValue().scalarMultiply(this.commandInverse ? -1 : 1);
                if (this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot().position().subtract(targetPos).getNorm() >= 20.0) {
                    this.moveMap.get(id).setPos(targetPos);
                    List<AbstractObstacle> obstacles = new ArrayList<>();
                    obstacles.addAll(CommonObstacles.getFriendlyRobotsIdsExcluded(this.world.get(), this.color, new int[]{id}, this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot(), CommonObstacles.getMarginRobot()));
                    obstacles.addAll(CommonObstacles.getOppositeRobots(this.world.get(), this.color, this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot(), CommonObstacles.getMarginRobot()));
                    obstacles.addAll(CommonObstacles.getFriendlyGoal(this.world.get().getField(), CommonObstacles.getMarginRobot()));
                    obstacles.addAll(CommonObstacles.getOppositeGoal(this.world.get().getField(), CommonObstacles.getMarginRobot()));
                    list.add(new WithPlanner<>(this.moveMap.get(id), id, this.color, obstacles));
                    tmpIds.remove((Integer) id);
                    isRemove = false;
                }
            }
            if(oc.getBallPos().isPresent()){
                Vector2D ballPos = this.world.get().getBall().position();
                List<IntegratedRobot> tmpRobots = MathHelper.robotListFromIds(this.world.get().getFriendlyRobotMap(this.color), tmpIds);
                Optional<IntegratedRobot> nearestRobot = MathHelper.nearestRobotToPosition(tmpRobots, ballPos);
                Vector2D abpTarget = oc.getBallPos().get().scalarMultiply(this.commandInverse ? -1 : 1);
                if(nearestRobot.isPresent()) {
                    int id = nearestRobot.get().getId();
                    this.ballPlaceMap.get(id).setAbpTarget(abpTarget);
                    if (!this.ballPlaceMap.get(id).isFinished() && ballPos.subtract(abpTarget).getNorm() >= 100) {
                        List<AbstractObstacle> obstacles = new ArrayList<>();
                        obstacles.addAll(CommonObstacles.getFriendlyRobotsIdsExcluded(this.world.get(), this.color, new int[]{id}, this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot(), CommonObstacles.getMarginRobot()));
                        obstacles.addAll(CommonObstacles.getOppositeRobots(this.world.get(), this.color, this.world.get().getFriendlyRobotMap(this.color).get(id).getRobot(), CommonObstacles.getMarginRobot()));
                        obstacles.addAll(CommonObstacles.getFriendlyGoal(this.world.get().getField(), CommonObstacles.getMarginRobot()));
                        obstacles.addAll(CommonObstacles.getOppositeGoal(this.world.get().getField(), CommonObstacles.getMarginRobot()));
                        list.add(new WithPlanner<>(this.ballPlaceMap.get(id), id, this.color, obstacles));
                        isRemove = false;
                        tmpIds.remove((Integer) id);
                    }
                }
            }
            if(isRemove) {
                UpdaterOptionalCommand.getInstance().remove(0);
            }
        }
        for (int id : tmpIds) {
            list.add(this.haltMap.get(id));
        }
        return list;
    }

    @Override
    public String getName() {
        return "locate";
    }

    public void setInverse(boolean value) {
        super.setInverse(value);
        this.commandInverse = value;
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
