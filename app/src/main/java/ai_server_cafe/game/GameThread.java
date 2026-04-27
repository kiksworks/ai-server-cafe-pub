package ai_server_cafe.game;

import ai_server_cafe.Main;
import ai_server_cafe.config.Config;
import ai_server_cafe.controller.PIDController;
import ai_server_cafe.filter.limited.FilterControlledRobot;
import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.action.ActionWrapper;
import ai_server_cafe.game.captain.AbstractCaptain;
import ai_server_cafe.game.formation.AbstractFormation;
import ai_server_cafe.game.planner.strategy.SimpleSearch;
import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.model.game.EnumCaptainType;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterThreadStatus;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.game.InstanceHelper;
import ai_server_cafe.util.thread.AbstractLoopThreadCafe;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GameThread extends AbstractLoopThreadCafe {
    private static GameThread instanceYellow = null;
    private static GameThread instanceBlue = null;
    private double lastDate;
    private long gameCycles;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<? extends AbstractCaptain> captain;
    private EnumCaptainType lastCaptainType;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<SimpleSearch> simpleSearch;
    private final TeamColor color;

    public static final boolean ENABLE_HALT_IN_NOT_RUNNING = true;

    /**
     * DO NOT USE THIS CONSTRUCTOR!!!
     * USE GameThread#getInstance()
      */
    private GameThread(TeamColor color) {
        super("game-runner-" + color);
        this.color = color;
        this.captain = Optional.empty();
        this.simpleSearch = Optional.empty();
        this.gameCycles = 0;
    }

    @Override
    protected void loop() {
        double nowDate = TimeHelper.now();
        ConfigManager configManager = ConfigManager.getInstance();
        if (nowDate - this.lastDate >= configManager.getCycleTime()) {
            Config config = configManager.getConfig();
            this.lastDate = nowDate;
            UpdaterThreadStatus.getInstance().addTPSData(this.name, nowDate);
            this.gameCycles++;
            try {
                Map<Integer, Command> commands = new HashMap<>();
                Map<Integer, AbstractAction> actionMap = new HashMap<>();
                Map<Integer, AbstractRole> roleMap = new HashMap<>();
                EnumCaptainType captainType = config.getCaptain(this.color);
                /* =============================== Check updates ==============================*/
                // このタイミングでcaptain先でUpdater系を参照してcaptainを初期化
                if (configManager.isNeedReset(this.color)) {
                    resetRobots(this.logger);
                    this.captain = InstanceHelper.makeCaptain(captainType.getCaptainClass(), captainType, this.color);
                    this.simpleSearch = Optional.of(new SimpleSearch(this.color));
                    configManager.setNeedReset(this.color, false);
                } else {
                    if (this.lastCaptainType != captainType) {
                        resetRobots(this.logger);
                        this.captain = InstanceHelper.makeCaptain(captainType.getCaptainClass(), captainType, this.color);
                    }
                }

                if (this.captain.isEmpty()) {
                    this.captain = InstanceHelper.makeCaptain(captainType.getCaptainClass(), captainType, this.color);
                }
                if (this.simpleSearch.isEmpty()) {
                    this.simpleSearch = Optional.of(new SimpleSearch(this.color));
                }

                this.updateControlType(false);

                if (configManager.isStart()) {
                    /* =============================== Run SimpleSearch ==============================*/
                    this.simpleSearch.get().execute();
                    /* =============================== Run captains ==============================*/
                    if (this.captain.isPresent()) {
                        AbstractCaptain captain = this.captain.get();
                        Optional<? extends AbstractFormation> optionalAbstractFormation = captain.update(this.gameCycles);
                        if (optionalAbstractFormation.isPresent()) {
                            List<? extends AbstractRole> roles = optionalAbstractFormation.get().update(this.gameCycles);
                            for (AbstractRole role : roles) {
                                List<? extends AbstractAction> actions = role.update(this.gameCycles);
                                for (AbstractAction action : actions) {
                                    Command command = action.update(this.gameCycles);
                                    commands.put(action.getId(), command);
                                    actionMap.put(action.getId(), new ActionWrapper(action.getId(), action.getColor(), action.getName(), action.isFinished(), command));
                                    roleMap.put(action.getId(), role);
                                }
                            }
                        } else {
                            this.logger.warn("formation was not set by blue captain : {}", captain.getType().getName());
                        }
                    }
                } else if (ENABLE_HALT_IN_NOT_RUNNING) {
                    this.captain = Optional.empty();
                    // Haltを送るだけなのでworldのinvertは関係ない
                    if (captainType != EnumCaptainType.NONE) {
                        commands.putAll(getHaltCommands(this.color, AbstractFormation.getVisibleIds(UpdaterWorld.getInstance()
                                .getWorld(false), this.color, config.getActiveRobots(this.color))));
                    }
                }
                this.lastCaptainType = captainType;
                /* ================ set command =============== */
                UpdaterWorld.getInstance().updateCommands(commands, this.color);
                UpdaterWorld.getInstance().updateActions(actionMap, this.color);
                UpdaterWorld.getInstance().updateRoles(roleMap, this.color);
            } catch(Exception e) {
                e.printStackTrace();
                this.logger.error("Unknown error occurred : {}", (Object[]) e.getStackTrace());
                Main.exit(1, true);
            }
        }
    }

    @Override
    protected void init() {
        this.lastDate = TimeHelper.now();
        this.gameCycles = 0;
        this.lastCaptainType = ConfigManager.getInstance().getConfig().getCaptain(this.color);
        resetRobots(this.logger);
    }

    private void resetRobots(@Nonnull Logger logger) {
        logger.info("reset robots");
        UpdaterWorld.getInstance().clearAllRobotMap(this.color);
        this.updateControlType(true);
        this.setFilter();
    }

    public static GameThread getInstance(@Nonnull TeamColor color) {
        if (color.isYellow()) {
            if (instanceYellow == null) {
                instanceYellow = new GameThread(color);
            }
            return instanceYellow;
        }
        if (instanceBlue == null) {
            instanceBlue = new GameThread(color);
        }
        return instanceBlue;
    }

    @Nonnull
    public static Map<Integer, Command> getHaltCommands(@Nonnull TeamColor color, @Nonnull List<Integer> activeRobots) {
        Map<Integer, Command> commandList = new HashMap<>();
        for (int id : activeRobots) {
            commandList.put(id, new Command().setHalt(true));
        }
        return commandList;
    }

    public static void reset() {
        instanceYellow = null;
        instanceBlue = null;
    }

    private void setFilter() {
        Config config = ConfigManager.getInstance().getConfig();
        if (this.doControl(this.color)) {
            for (int id : config.getActiveRobots(this.color)) {
                UpdaterWorld.getInstance().setFilterRobot(this.color, id, new FilterControlledRobot(
                        config.getCycleTime(),
                        config.filterConfig.controlDelay,
                        config.filterConfig.lostDuration
                ));
            }
        }
    }

    private void updateControlType(boolean forceUpdate) {
        if (forceUpdate) {
            Config config = ConfigManager.getInstance().getConfig();
            if (this.doControl(this.color)) {
                for (int id : config.getActiveRobots(this.color)) {
                    UpdaterWorld.getInstance().setController(this.color, id,
                            new PIDController(config.getCycleTime(), config.controllerConfig));
                }
            }
        }
    }

    private boolean doControl(TeamColor color) {
        Config config = ConfigManager.getInstance().getConfig();
        return config.getCaptain(color) != EnumCaptainType.NONE;
    }
}
