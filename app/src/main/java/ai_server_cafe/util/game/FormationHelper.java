package ai_server_cafe.util.game;

import ai_server_cafe.model.game.EnumFormationType;
import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.Tuple;

import javax.annotation.Nonnull;

public class FormationHelper {
    /**
     *
     * @param command
     * @param prevCommand
     * @param currentStage
     * @param color
     * @return formation type, isOurBall, isPrepare
     */
    @Nonnull
    public static Tuple<EnumFormationType, Boolean, Boolean> getFromGcCommand(GcRefereeMessage.Referee.Command command, GcRefereeMessage.Referee.Command prevCommand, GcRefereeMessage.Referee.Stage currentStage, TeamColor color) {
        // TODO: 処理を追加
        if (command == GcRefereeMessage.Referee.Command.HALT)
            return new Tuple<>(EnumFormationType.HALT, false, false);
        else if (command == GcRefereeMessage.Referee.Command.STOP)
            return new Tuple<>(EnumFormationType.STOP, false, false);
        else if (command == GcRefereeMessage.Referee.Command.FORCE_START)
            return new Tuple<>(EnumFormationType.FORCE_START, false, false);
        else if (command == GcRefereeMessage.Referee.Command.TIMEOUT_BLUE)
            return new Tuple<>(EnumFormationType.TIMEOUT, !color.isYellow(), false);
        else if (command == GcRefereeMessage.Referee.Command.TIMEOUT_YELLOW)
            return new Tuple<>(EnumFormationType.TIMEOUT, color.isYellow(), false);
        else if (command == GcRefereeMessage.Referee.Command.BALL_PLACEMENT_BLUE)
            return new Tuple<>(EnumFormationType.BALL_PLACEMENT, !color.isYellow(), false);
        else if (command == GcRefereeMessage.Referee.Command.BALL_PLACEMENT_YELLOW)
            return new Tuple<>(EnumFormationType.BALL_PLACEMENT, color.isYellow(), false);
        else if (command == GcRefereeMessage.Referee.Command.DIRECT_FREE_BLUE || command == GcRefereeMessage.Referee.Command.INDIRECT_FREE_BLUE)
            return new Tuple<>(EnumFormationType.SET_PLAY, !color.isYellow(), false);
        else if (command == GcRefereeMessage.Referee.Command.DIRECT_FREE_YELLOW || command == GcRefereeMessage.Referee.Command.INDIRECT_FREE_YELLOW)
            return new Tuple<>(EnumFormationType.SET_PLAY, color.isYellow(), false);
        else if (command == GcRefereeMessage.Referee.Command.PREPARE_KICKOFF_BLUE)
            return new Tuple<>(EnumFormationType.KICKOFF, !color.isYellow(), true);
        else if (command == GcRefereeMessage.Referee.Command.PREPARE_KICKOFF_YELLOW)
            return new Tuple<>(EnumFormationType.KICKOFF, color.isYellow(), true);
        else if (command == GcRefereeMessage.Referee.Command.PREPARE_PENALTY_BLUE)
            if (currentStage != GcRefereeMessage.Referee.Stage.PENALTY_SHOOTOUT)
                return new Tuple<>(EnumFormationType.PENALTY, !color.isYellow(), true);
            else
                return new Tuple<>(EnumFormationType.SHOOTOUT, !color.isYellow(), true);
        else if (command == GcRefereeMessage.Referee.Command.PREPARE_PENALTY_YELLOW)
            if (currentStage != GcRefereeMessage.Referee.Stage.PENALTY_SHOOTOUT)
                return new Tuple<>(EnumFormationType.PENALTY, color.isYellow(), true);
            else
                return new Tuple<>(EnumFormationType.SHOOTOUT, color.isYellow(), true);
        else if (command == GcRefereeMessage.Referee.Command.NORMAL_START) {
            Tuple<EnumFormationType, Boolean, Boolean> prevTuple = getFromGcCommand(prevCommand, prevCommand, currentStage, color);
            return new Tuple<>(prevTuple.getFirst(), prevTuple.getSecond(), false);
        }
        return new Tuple<>(EnumFormationType.UNKNOWN, false, false);
    }
}
