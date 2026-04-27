package ai_server_cafe.updater;

import ai_server_cafe.model.game.TeamInfoWrapper;
import ai_server_cafe.network.proto.ssl.gc.GcCommon;
import ai_server_cafe.network.proto.ssl.gc.GcGameEvent;
import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.WrapperWeakCloneable;
import ai_server_cafe.util.interfaces.WrapperWeakCloneableList;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public final class UpdaterRefBox {
    private static UpdaterRefBox instance = null;

    private GcRefereeMessage.Referee.Command currentCommand;
    private GcRefereeMessage.Referee.Stage currentStage;
    private Vector2D ballPlacePos;
    private int size;
    private Optional<GcRefereeMessage.Referee.TeamInfo> blueTeamInfo;
    private Optional<GcRefereeMessage.Referee.TeamInfo> yellowTeamInfo;
    private Optional<GcRefereeMessage.Referee.TeamInfo> globalKIKSTeamInfo;
    private Optional<TeamColor> globalKIKSTeamColor;
    private Optional<Boolean> isBluePositive;
    private final Deque<String> gcInfo;
    private boolean lastLocalRef;

    private int replaySize;
    private GcRefereeMessage.Referee.Command replayCommand;
    private GcRefereeMessage.Referee.Stage replayStage;
    private Vector2D replayBallPlacePos;
    private Optional<GcRefereeMessage.Referee.TeamInfo> replayBlueTeamInfo;
    private Optional<GcRefereeMessage.Referee.TeamInfo> replayYellowTeamInfo;
    private Optional<GcRefereeMessage.Referee.TeamInfo> replayGlobalKIKSTeamInfo;
    private Optional<TeamColor> replayGlobalKIKSTeamColor;
    private Optional<Boolean> replayIsBluePositive;
    private final Deque<String> replayGcInfo;
    private boolean replayLastLocalRef;
    private boolean replayMode;
    private final List<GcRefereeMessage.Referee> messages;
    private boolean isPopped;

    private UpdaterRefBox() {
        this.currentCommand = GcRefereeMessage.Referee.Command.HALT;
        this.currentStage = GcRefereeMessage.Referee.Stage.NORMAL_FIRST_HALF;
        this.ballPlacePos = Vector2D.ZERO;
        this.size = 0;
        this.blueTeamInfo = Optional.empty();
        this.yellowTeamInfo = Optional.empty();
        this.globalKIKSTeamInfo = Optional.empty();
        this.globalKIKSTeamColor = Optional.empty();
        this.gcInfo = new ArrayDeque<>();
        this.lastLocalRef = false;

        this.messages = new ArrayList<>();
        this.isPopped = false;
        this.replaySize = 0;
        this.replayCommand = GcRefereeMessage.Referee.Command.HALT;
        this.replayStage = GcRefereeMessage.Referee.Stage.NORMAL_FIRST_HALF;
        this.replayBallPlacePos = Vector2D.ZERO;
        this.replayBlueTeamInfo = Optional.empty();
        this.replayYellowTeamInfo = Optional.empty();
        this.replayGlobalKIKSTeamInfo = Optional.empty();
        this.replayGlobalKIKSTeamColor = Optional.empty();
        this.replayIsBluePositive = Optional.empty();
        this.replayGcInfo = new ArrayDeque<>();
        this.replayLastLocalRef = false;
        this.replayMode = false;
    }

    synchronized public void updateRefBox(@Nullable GcRefereeMessage.Referee referee, int size) {
        this.size = size;
        this.replaySize = size;
        if (referee == null) {
            return;
        }
        if (this.isPopped) {
            this.messages.clear();
            this.isPopped = false;
        }
        this.messages.addLast(referee);
        if (referee.hasBlue()) {
            if (referee.getBlue().getName().equals("KIKS")) {
                this.globalKIKSTeamInfo = Optional.of(referee.getBlue());
                this.globalKIKSTeamColor = Optional.of(TeamColor.BLUE);
            }
        }
        if (referee.hasYellow()) {
            if (referee.getYellow().getName().equals("KIKS")) {
                this.globalKIKSTeamInfo = Optional.of(referee.getYellow());
                this.globalKIKSTeamColor = Optional.of(TeamColor.YELLOW);
            }
        }
        if (referee.hasBlueTeamOnPositiveHalf()) {
            this.isBluePositive = Optional.of(referee.getBlueTeamOnPositiveHalf());
        }
        if (!ConfigManager.getInstance().isUseLocalRef()) {
            if (referee.hasDesignatedPosition()) {
                this.ballPlacePos = new Vector2D(referee.getDesignatedPosition().getX(), referee.getDesignatedPosition().getY());
            }
            if (referee.hasBlue()) {
                this.blueTeamInfo = Optional.of(referee.getBlue());
            }
            if (referee.hasYellow()) {
                this.yellowTeamInfo = Optional.of(referee.getYellow());
            }
            if (referee.hasCommand()) {
                this.currentCommand = referee.getCommand();
            }
            if (referee.hasStage()) {
                this.currentStage = referee.getStage();
            }
        }
        this.gcInfo.clear();
        this.setGcInfo(referee, this.gcInfo);
    }

    synchronized public void replayStop() {
        this.replayGcInfo.clear();
        this.isPopped = false;
        this.replaySize = 0;
        this.replayCommand = GcRefereeMessage.Referee.Command.HALT;
        this.replayStage = GcRefereeMessage.Referee.Stage.NORMAL_FIRST_HALF;
        this.replayBallPlacePos = Vector2D.ZERO;
        this.replayBlueTeamInfo = Optional.empty();
        this.replayYellowTeamInfo = Optional.empty();
        this.replayGlobalKIKSTeamInfo = Optional.empty();
        this.replayGlobalKIKSTeamColor = Optional.empty();
        this.replayIsBluePositive = Optional.empty();
        this.replayLastLocalRef = false;
        this.replayMode = false;
    }

    synchronized public void updateRefBoxByReplay(@Nullable GcRefereeMessage.Referee referee) {
        this.replayMode = true;
        if (referee == null) {
            return;
        }
        if (referee.hasBlue()) {
            if (referee.getBlue().getName().equals("KIKS")) {
                this.replayGlobalKIKSTeamInfo = Optional.of(referee.getBlue());
                this.replayGlobalKIKSTeamColor = Optional.of(TeamColor.BLUE);
            }
        }
        if (referee.hasYellow()) {
            if (referee.getYellow().getName().equals("KIKS")) {
                this.replayGlobalKIKSTeamInfo = Optional.of(referee.getYellow());
                this.replayGlobalKIKSTeamColor = Optional.of(TeamColor.YELLOW);
            }
        }
        if (referee.hasBlueTeamOnPositiveHalf()) {
            this.replayIsBluePositive = Optional.of(referee.getBlueTeamOnPositiveHalf());
        }
        if (referee.hasDesignatedPosition()) {
            this.replayBallPlacePos = new Vector2D(referee.getDesignatedPosition().getX(), referee.getDesignatedPosition().getY());
        }
        if (referee.hasBlue()) {
            this.replayBlueTeamInfo = Optional.of(referee.getBlue());
        }
        if (referee.hasYellow()) {
            this.replayYellowTeamInfo = Optional.of(referee.getYellow());
        }
        if (referee.hasCommand()) {
            this.replayCommand = referee.getCommand();
        }
        if (referee.hasStage()) {
            this.replayStage = referee.getStage();
        }
        this.replayGcInfo.clear();
        this.setGcInfo(referee, this.replayGcInfo);
    }

    private void setGcInfo(@Nonnull GcRefereeMessage.Referee referee, Deque<String> gcInfo) {
        for(GcGameEvent.GameEvent event : referee.getGameEventsList()) {
            //"Bot dropped parts"はコード内に無いので受け取れない
            if(event.hasBallLeftFieldTouchLine()) {
                gcInfo.add("BallLeftField - "
                        + event.getBallLeftFieldTouchLine().getByTeam()
                        + event.getBallLeftFieldTouchLine().getByBot());
            }
            else if(event.hasBallLeftFieldGoalLine()) {
                gcInfo.add("BallLeftField - "
                        + event.getBallLeftFieldGoalLine().getByTeam()
                        + event.getBallLeftFieldGoalLine().getByBot());
            }
            else if (event.hasAimlessKick()) {
                gcInfo.add("AimlessKick - "
                        + event.getAimlessKick().getByTeam()
                        + event.getAimlessKick().getByBot());
            }
            else if (event.hasGoal()) {
                gcInfo.add("Goal"
                        + event.getGoal().getByTeam()
                        + " shot " + event.getGoal().getKickingTeam()
                        + event.getGoal().getKickingBot()
                        + ":" + event.getGoal().getMessage());
            }
            else if (event.hasIndirectGoal()) {
                gcInfo.add("IndirectGoal"
                        + event.getIndirectGoal().getByTeam()
                        + event.getIndirectGoal().getByBot() + " shot");
            }
            else if(event.hasChippedGoal()) {
                gcInfo.add("ChippedGoal"
                        + event.getChippedGoal().getByTeam()
                        + event.getChippedGoal().getByBot()
                        + " shot height:" +event.getChippedGoal().getMaxBallHeight() + "[m]");
            }
            else if (event.hasBotTooFastInStop()) {
                gcInfo.add("BotTooFastInStop - "
                        + event.getBotTooFastInStop().getByTeam()
                        + event.getBotTooFastInStop().getByBot()
                        + " at " + event.getBotTooFastInStop().getSpeed() + "[m/s]");
            }
            else if (event.hasDefenderTooCloseToKickPoint()) {
                gcInfo.add("DefenderTooCloseToKickPoint - "
                        + event.getDefenderTooCloseToKickPoint().getByTeam()
                        + event.getDefenderTooCloseToKickPoint().getByBot());
            }
            else if (event.hasBotCrashDrawn()) {
                gcInfo.add("BotCrashedDrawn - "
                        + "Yellow" + event.getBotCrashDrawn().getBotBlue()
                        + "Blue" + event.getBotCrashDrawn().getBotYellow());
            }
            else if(event.hasBotCrashUnique()) {
                gcInfo.add("BotCrashUnique - "
                        + (event.getBotCrashUnique().getByTeam() == GcCommon.Team.BLUE ? "BLUE" : "YELLOW")
                        + event.getBotCrashUnique().getViolator() + " crashed to "
                        + (event.getBotCrashUnique().getByTeam() == GcCommon.Team.BLUE ? "YELLOW" : "BLUE")
                        + event.getBotCrashUnique().getVictim());
            }
            else if(event.hasBotPushedBot()) {
                gcInfo.add("BotPushedBot - "
                        + (event.getBotPushedBot().getByTeam() == GcCommon.Team.BLUE ? "BLUE" : "YELLOW")
                        + event.getBotPushedBot().getViolator() + " pushed to "
                        + (event.getBotPushedBot().getByTeam() == GcCommon.Team.BLUE ? "YELLOW" : "BLUE")
                        + event.getBotPushedBot().getVictim());
            }
            else if (event.hasBotTippedOver()) {
                gcInfo.add("BotTippedOver - "
                        + event.getBotTippedOver().getByTeam()
                        + event.getBotTippedOver().getByBot());
            }
            else if (event.hasDefenderInDefenseArea()) {
                gcInfo.add("DefenderInDefenseArea - "
                        + event.getDefenderInDefenseArea().getByTeam()
                        + event.getDefenderInDefenseArea().getByBot());
            }
            else if (event.hasAttackerTouchedBallInDefenseArea()) {
                gcInfo.add("AttackerTouchedBallInDefenseArea - "
                        + event.getAttackerTouchedBallInDefenseArea().getByTeam()
                        + event.getAttackerTouchedBallInDefenseArea().getByBot());
            }
            else if(event.hasAttackerTouchedOpponentInDefenseArea()) {
                //なぜかgetByTeamが得られない
                gcInfo.add("AttackerTouchedOpponentInDefenseArea - "
                        + event.getAttackerTouchedBallInDefenseArea().getByTeam()
                        + event.getAttackerTouchedBallInDefenseArea().getByBot());
            }
            else if(event.hasBotKickedBallTooFast()) {
                gcInfo.add("BotKickedBallTooFast - "
                        + event.getBotKickedBallTooFast().getByTeam()
                        + event.getBotKickedBallTooFast().getByBot()
                        + event.getBotKickedBallTooFast().getInitialBallSpeed() + "[m/s] "
                        + (event.getBotKickedBallTooFast().hasChipped() ? "chipkick" : ""));
            }
            else if(event.hasBotDribbledBallTooFar()) {
                gcInfo.add("BotDribbledBallTooFar - "
                        + event.getBotDribbledBallTooFar().getByTeam()
                        + event.getBotDribbledBallTooFar().getByBot());
            }
            else if(event.hasAttackerTooCloseToDefenseArea()) {
                gcInfo.add("AttackerTooCloseToDefenseArea - "
                        + event.getAttackerTooCloseToDefenseArea().getByTeam()
                        + event.getAttackerTooCloseToDefenseArea().getByBot());
            }
            else if(event.hasBotHeldBallDeliberately()) {
                gcInfo.add("BotHeldBallDeliberately - "
                        + event.getBotHeldBallDeliberately().getByTeam()
                        + event.getBotHeldBallDeliberately().getByBot());
            }
            else if(event.hasBotInterferedPlacement()) {
                gcInfo.add("BotInterferedPlacement - "
                        + event.getBotInterferedPlacement().getByTeam()
                        + event.getBotInterferedPlacement().getByBot());
            }
            else if(event.hasMultipleCards()) {
                gcInfo.add("MultipleCards - " + event.getMultipleCards().getByTeam());
            }
            else if(event.hasMultipleFouls()) {
                gcInfo.add("MultipleFouls - " + event.getMultipleFouls().getByTeam());
            }
            else if(event.hasMultiplePlacementFailures()) {
                gcInfo.add("MultiplePlacementFailures - " + event.getMultiplePlacementFailures().getByTeam());
            }
            else if(event.hasKickTimeout()) {
                gcInfo.add("KickTimeout - " + event.getKickTimeout().getByTeam());
            }
            else if(event.hasNoProgressInGame()) {
                gcInfo.add("NoProgressInGame - ");
            }
            else if(event.hasPlacementFailed()) {
                gcInfo.add("PlacementFailed - " + event.getPlacementFailed().getByTeam());
            }
            else if(event.hasUnsportingBehaviorMinor()) {
                gcInfo.add("UnsportingBehaviorMinor - "
                        + event.getUnsportingBehaviorMinor().getByTeam()
                        + event.getUnsportingBehaviorMinor().getReason());
            }
            else if(event.hasUnsportingBehaviorMajor()) {
                gcInfo.add("UnsportingBehaviorMajor - "
                        + event.getUnsportingBehaviorMajor().getByTeam()
                        + event.getUnsportingBehaviorMajor().getReason());
            }
            else if(event.hasPlacementSucceeded()) {
                gcInfo.add("PlacementSucceeded - " + event.getPlacementSucceeded().getByTeam());
            }
            else if(event.hasPrepared()) {
                gcInfo.add("Prepared - " + event.getPrepared().getTimeTaken() + "s");
            }
            else if(event.hasBotSubstitution()) {
                gcInfo.add("BotSubstitution - " + event.getBotSubstitution().getByTeam());
            }
            else if(event.hasChallengeFlag()) {
                gcInfo.add("ChallengeFlag - " + event.getChallengeFlag().getByTeam());
            }
            else if(event.hasEmergencyStop()) {
                gcInfo.add("EmergencyStop - " + event.getEmergencyStop().getByTeam());
            }
            else if(event.hasTooManyRobots()) {
                gcInfo.add("TooManyRobots - " + event.getTooManyRobots().getByTeam() + " has " + (event.getTooManyRobots().getNumRobotsOnField() - event.getTooManyRobots().getNumRobotsAllowed()) + "more robots.");
            }
            else if(event.hasBoundaryCrossing()) {
                gcInfo.add("BoundaryCrossing - " + event.getBoundaryCrossing().getByTeam());
            }
            else if(event.hasPenaltyKickFailed()) {
                gcInfo.add("PenaltyKickFailed" + event.getPenaltyKickFailed().getByTeam());
            }
        }
    }

    synchronized public void updateLocalRefBox(@Nonnull GcRefereeMessage.Referee referee) {
        if (ConfigManager.getInstance().isUseLocalRef()) {
            if (referee.hasDesignatedPosition()) {
                this.ballPlacePos = new Vector2D(referee.getDesignatedPosition().getX(), referee.getDesignatedPosition().getY());
            }
            if (referee.hasBlue()) {
                this.blueTeamInfo = Optional.of(referee.getBlue());
            }
            if (referee.hasYellow()) {
                this.yellowTeamInfo = Optional.of(referee.getYellow());
            }
            if (referee.hasCommand()) {
                this.currentCommand = referee.getCommand();
            }
            if (referee.hasStage()) {
                this.currentStage = referee.getStage();
            }
        } else if (this.lastLocalRef) {
            this.currentCommand = GcRefereeMessage.Referee.Command.HALT;
        }
        this.lastLocalRef = ConfigManager.getInstance().isUseLocalRef();
    }

    synchronized public GcRefereeMessage.Referee.Command getCurrentCommand() {
        if (this.replayMode) {
            return this.replayCommand;
        }
        return this.currentCommand;
    }

    @Nonnull
    synchronized public WrapperWeakCloneable<Vector2D> getBallPlacePos() {
        if (this.replayMode) {
            return new WrapperWeakCloneable<>(this.replayBallPlacePos);
        }
        return new WrapperWeakCloneable<>(this.ballPlacePos);
    }

    synchronized public GcRefereeMessage.Referee.Stage getCurrentStage() {
        if (this.replayMode) {
            return this.replayStage;
        }
        return this.currentStage;
    }

    synchronized public int getUpdatingPerSec() {
        return this.size;
    }

    @Nonnull
    synchronized public TeamInfoWrapper getTeamInfo(@Nonnull TeamColor color) {
        if (this.replayMode) {
            if (color.isYellow())
                return new TeamInfoWrapper(this.replayYellowTeamInfo);
            return new TeamInfoWrapper(this.replayBlueTeamInfo);
        }
        if (color.isYellow())
            return new TeamInfoWrapper(this.yellowTeamInfo);
        return new TeamInfoWrapper(this.blueTeamInfo);
    }

    @Nonnull
    synchronized public WrapperWeakCloneable<Optional<GcRefereeMessage.Referee.TeamInfo>> getGlobalKIKSInfo() {
        if (this.replayMode) {
            return new WrapperWeakCloneable<>(this.replayGlobalKIKSTeamInfo);
        }
        return new WrapperWeakCloneable<>(this.globalKIKSTeamInfo);
    }

    @Nonnull
    synchronized public WrapperWeakCloneable<Optional<TeamColor>> getGlobalKIKSColor() {
        if (this.replayMode) {
            return new WrapperWeakCloneable<>(this.replayGlobalKIKSTeamColor);
        }
        return new WrapperWeakCloneable<>(this.globalKIKSTeamColor);
    }

    @Nonnull
    synchronized public WrapperWeakCloneable<Optional<Boolean>> getIsBluePositive() {
        if (this.replayMode) {
            return new WrapperWeakCloneable<>(this.replayIsBluePositive);
        }
        return new WrapperWeakCloneable<>(this.isBluePositive);
    }

    public synchronized static UpdaterRefBox getInstance() {
        if (instance == null) {
            instance = new UpdaterRefBox();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    @Nonnull
    synchronized public WrapperWeakCloneable<String> getGcInfo() {
        //gcInfoが2000行以上だったら上から順にstashする
        while (this.gcInfo.size() >= 2000) {
            this.gcInfo.removeFirst();
        }
        System.gc();
        StringBuilder result = new StringBuilder();
        for (String s : this.gcInfo) {
            result.append(s).append("\n");
        }
        return new WrapperWeakCloneable<>(result.toString());
    }
    /**
     * </b>REPLAY用 DO NOT USE!!!!
     * @return
     */
    @Nonnull
    synchronized public WrapperWeakCloneableList<GcRefereeMessage.Referee> getMessagesAndPop() {
        this.isPopped = true;
        return new WrapperWeakCloneableList<>(this.messages);
    }
}
