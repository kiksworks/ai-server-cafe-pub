package ai_server_cafe.model.network;

import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.AbstractCloneable;
import ai_server_cafe.util.interfaces.MapLikeRobotList;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.Pair;

import java.util.Map;
import java.util.Optional;

public class OptionalCommand extends AbstractCloneable {
    private Optional<Vector2D> ballPos;
    private Optional<Vector2D> ballVel;
    private MapLikeRobotList<Vector2D> robotPos;

    public OptionalCommand() {
        this.ballPos = Optional.empty();
        this.ballVel = Optional.empty();
        this.robotPos = new MapLikeRobotList<>();
    }

    public OptionalCommand setBallPos(Vector2D ballPos) {
        this.ballPos = Optional.of(ballPos);
        return this;
    }

    public OptionalCommand setBallVel(Vector2D ballVel) {
        this.ballVel = Optional.of(ballVel);
        return this;
    }

    public OptionalCommand setRobotPos(TeamColor color, int id, Vector2D pos) {
        this.robotPos.put(color, id, pos);
        return this;
    }

    public Optional<Vector2D> getBallPos() {
        return this.ballPos;
    }

    public Optional<Vector2D> getBallVel() {
        return this.ballVel;
    }

    public MapLikeRobotList<Vector2D> getRobotPos() {
        return this.robotPos;
    }

    /**
     * 別の OptionalCommand を結合させる
     * すでに値がある場合は上書き
     * @param command 結合させるOptionalCommand
     * @return 自身のインスタンス
     */
    public OptionalCommand combine(OptionalCommand command) {
        if (command.getBallPos().isPresent())
            this.setBallPos(command.getBallPos().get());
        if (command.getBallVel().isPresent())
            this.setBallVel(command.getBallVel().get());
        for (Map.Entry<Pair<TeamColor, Integer>, Vector2D> entry : command.getRobotPos().entrySet())
            this.setRobotPos(entry.getKey().getFirst(), entry.getKey().getSecond(), entry.getValue());
        return this;
    }

    @Override
    public OptionalCommand clone() {
        OptionalCommand command = new OptionalCommand();
        if(this.ballPos.isPresent()) {
            command.setBallPos(this.ballPos.get());
        }
        if(this.ballVel.isPresent()) {
            command.setBallVel(this.ballVel.get());
        }
        command.robotPos = new MapLikeRobotList<>(this.robotPos.getMap(TeamColor.BLUE), this.robotPos.getMap(TeamColor.YELLOW));
        return command;
    }
}
