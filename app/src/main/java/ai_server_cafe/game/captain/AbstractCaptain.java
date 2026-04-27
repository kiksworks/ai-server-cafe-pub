package ai_server_cafe.game.captain;

import ai_server_cafe.config.Config;
import ai_server_cafe.game.formation.AbstractFormation;
import ai_server_cafe.game.formation.FormationHalt;
import ai_server_cafe.model.game.EnumCaptainType;
import ai_server_cafe.model.game.EnumFormationType;
import ai_server_cafe.network.proto.ssl.gc.GcRefereeMessage;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterRefBox;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.game.FormationHelper;
import ai_server_cafe.util.game.InstanceHelper;
import ai_server_cafe.util.interfaces.Tuple;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractCaptain {
    private Optional<? extends AbstractFormation> currentFormation;
    private Map<EnumFormationType, Class<? extends AbstractFormation>> formationMap;
    private Map<EnumFormationType, Double> velocityMap;
    private GcRefereeMessage.Referee.Command prevCommand;
    private GcRefereeMessage.Referee.Command lastCommand;
    protected final EnumCaptainType type;
    protected final TeamColor color;
    protected EnumFormationType prevFormationType;

    public AbstractCaptain(EnumCaptainType type, TeamColor color) {
        this.formationMap = new HashMap<>();
        this.setFormationMap(this.formationMap);
        this.velocityMap = new HashMap<>();
        this.setVelocityMap(this.velocityMap);
        this.prevCommand = GcRefereeMessage.Referee.Command.HALT;
        this.lastCommand = GcRefereeMessage.Referee.Command.HALT;
        this.currentFormation = Optional.empty();
        this.type = type;
        this.color = color;
        this.prevFormationType = EnumFormationType.HALT;
    }

    public Optional<? extends AbstractFormation> update(long tick) {
        GcRefereeMessage.Referee.Command command = UpdaterRefBox.getInstance().getCurrentCommand();
        if (this.lastCommand != command) {
            this.prevCommand = this.lastCommand;
        }
        this.lastCommand = command;
        Config config = ConfigManager.getInstance().getConfig();
        Tuple<EnumFormationType, Boolean, Boolean> tuple = FormationHelper.getFromGcCommand(command, this.prevCommand, UpdaterRefBox.getInstance().getCurrentStage(), this.color);
        EnumFormationType currentFormationType = tuple.getFirst();
        // 処理
        if (this.currentFormation.isEmpty() || currentFormationType != this.prevFormationType) {
            // 初期化
            this.currentFormation = Optional.empty();
            this.currentFormation = Optional.of(InstanceHelper.makeFormation(this.formationMap.getOrDefault(currentFormationType,
                            FormationHalt.class), this.color, config.getActiveRobots(this.color)));
        } else if (this.currentFormation.isPresent() && this.currentFormation.get().isFinished() && command == GcRefereeMessage.Referee.Command.NORMAL_START) {
            // 初期化
            this.currentFormation = Optional.empty();
            this.currentFormation = Optional.of(InstanceHelper.makeFormation(this.formationMap.getOrDefault(EnumFormationType.FORCE_START,
                    FormationHalt.class), this.color, config.getActiveRobots(this.color)));
        } else if (this.currentFormation.isPresent() && this.currentFormation.get().isFinished()
                && (command == GcRefereeMessage.Referee.Command.DIRECT_FREE_BLUE ||
                command == GcRefereeMessage.Referee.Command.DIRECT_FREE_YELLOW || command == GcRefereeMessage.Referee.Command.INDIRECT_FREE_BLUE ||  command == GcRefereeMessage.Referee.Command.INDIRECT_FREE_YELLOW)) {
            // 初期化
            this.currentFormation = Optional.empty();
            this.currentFormation = Optional.of(InstanceHelper.makeFormation(this.formationMap.getOrDefault(EnumFormationType.FORCE_START,
                    FormationHalt.class), this.color, config.getActiveRobots(this.color)));
        }
        double velocityLimit = this.velocityMap.getOrDefault(currentFormationType, config.controllerConfig.velocityMax);
        UpdaterWorld.getInstance().setVelocityLimit(velocityLimit);
        boolean invert = ConfigManager.getInstance().isInvert(this.color);
        AbstractFormation formation = this.currentFormation.get();
        // setWorld
        formation.setWorld(UpdaterWorld.getInstance().getWorld(invert));
        formation.setInverse(invert);
        Vector2D ballPlacePos = UpdaterRefBox.getInstance().getBallPlacePos().get();
        formation.setTeamInfo(UpdaterRefBox.getInstance().getTeamInfo(this.color).getInfo());
        formation.setBallPlacePos(ballPlacePos.scalarMultiply(invert ? -1.0 : 1.0));
        formation.setOurBall(tuple.getSecond());
        formation.setPrepare(tuple.getThird());

        this.prevFormationType = currentFormationType;
        return this.currentFormation;
    }

    /**
     * RefBoxから生成されたformationTypeとFormationクラスのmapを設定する
     * @param map 初期化された map
     */
    protected abstract void setFormationMap(Map<EnumFormationType, Class<? extends AbstractFormation>> map);

    /**
     * RefBoxから生成されたformationTypeとルールによる制限速度のmapを設定する
     * @param map 初期化された map
     */
    protected abstract void setVelocityMap(Map<EnumFormationType, Double> map);

    public final EnumCaptainType getType() {
        return this.type;
    }
}
