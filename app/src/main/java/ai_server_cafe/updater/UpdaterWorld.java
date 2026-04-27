package ai_server_cafe.updater;

import ai_server_cafe.controller.AbstractController;
import ai_server_cafe.filter.AbstractFilter;
import ai_server_cafe.filter.AbstractFilterManual;
import ai_server_cafe.filter.AbstractFilterSame;
import ai_server_cafe.filter.IObserver;
import ai_server_cafe.filter.limited.FilterBall;
import ai_server_cafe.game.action.AbstractAction;
import ai_server_cafe.game.role.AbstractRole;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.IntegratedRobot;
import ai_server_cafe.model.field.RawBall;
import ai_server_cafe.model.field.RawRobot;
import ai_server_cafe.model.field.World;
import ai_server_cafe.model.game.Command;
import ai_server_cafe.model.network.SendCommand;
import ai_server_cafe.network.proto.ssl.gc.GcCommon;
import ai_server_cafe.network.proto.ssl.vision.VisionDetection;
import ai_server_cafe.network.proto.ssl.vision.VisionDetectionTracked;
import ai_server_cafe.network.proto.ssl.vision.VisionGeometry;
import ai_server_cafe.network.proto.ssl.vision.VisionWrapper;
import ai_server_cafe.network.proto.ssl.vision.VisionWrapperTracked;
import ai_server_cafe.network.transmitter.RobotDriver;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.interfaces.MapLikeRobotList;
import ai_server_cafe.util.interfaces.WrapperCloneableList;
import ai_server_cafe.util.interfaces.WrapperCloneableMap;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class UpdaterWorld {
    public static final boolean CHIP_DETECTOR = false;

    private static UpdaterWorld instance = null;
    private final UpdaterBall updaterBall;
    private final Field field;
	private long commonCycle;
    private int visionPerSec;
    private int trackerPerSec;
    private final MapLikeRobotList<UpdaterRobot> updaterRobotMap;
    private final MapLikeRobotList<Command> commandMap;
    private final MapLikeRobotList<AbstractAction> actionMap;
    private final MapLikeRobotList<AbstractRole> roleMap;
    private final MapLikeRobotList<AbstractController> controllerMap;
    private Vector2D blueDribbleStartPos;
    private Vector2D yellowDribbleStartPos;
    private boolean blueHaveBall;
    private boolean yellowHaveBall;
    private final Map<Integer, VisionGeometry.CameraCalibration> cameraCalibrationMap;
    private boolean prevVisionDetected;
    private boolean isController;
    private Optional<World> replayWorld;
    private final Logger logger = LogManager.getLogger("world updater");

    private UpdaterWorld() {
        this.field = new Field();
        this.updaterRobotMap = new MapLikeRobotList<>();
        this.commandMap = new MapLikeRobotList<>();
        this.actionMap = new MapLikeRobotList<>();
        this.roleMap = new MapLikeRobotList<>();
        this.controllerMap = new MapLikeRobotList<>();
        this.updaterBall = new UpdaterBall();
        double lostDuration = ConfigManager.getInstance().getConfig().filterConfig.lostDuration;
        this.updaterBall.setFilterSame(new FilterBall(lostDuration));
        this.visionPerSec = 0;
        this.trackerPerSec = 0;
        this.blueDribbleStartPos = Vector2D.ZERO;
        this.yellowDribbleStartPos = Vector2D.ZERO;
        this.blueHaveBall = false;
        this.yellowHaveBall = false;
        this.commonCycle = 0;
        this.isController = true;
        this.prevVisionDetected = false;
        this.replayWorld = Optional.empty();
        this.cameraCalibrationMap = new HashMap<>();
    }

    /**
     * インスタンスファクトリ
     * @return instance
     */
    public synchronized static UpdaterWorld getInstance() {
        if (instance == null) {
            instance = new UpdaterWorld();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    /* ========================= 以下 setter =========================*/

    synchronized public void updateCommonCycle(long cycle) {
        this.commonCycle = cycle;
    }

    synchronized public void updateVision(@Nullable VisionWrapper.Packet packet, int perSec) {
        this.visionPerSec = perSec;
        if (packet == null) {
            this.prevVisionDetected = false;
            if (!ConfigManager.getInstance().getConfig().useTracker) {
                for (UpdaterRobot ur : this.updaterRobotMap.values()) {
                    ur.update((VisionDetection.Frame) null);
                }
            }
            return;
        }
        // update field
        if (packet.hasGeometry()) {
            if (packet.getGeometry().getField().hasFieldWidth()) this.field.setGameHeight(packet.getGeometry().getField().getFieldWidth());
            if (packet.getGeometry().getField().hasFieldLength()) this.field.setGameWidth(packet.getGeometry().getField().getFieldLength());
            if (packet.getGeometry().getField().hasPenaltyAreaWidth()) this.field.setPenaltyWidth(packet.getGeometry().getField().getPenaltyAreaWidth());
            if (packet.getGeometry().getField().hasPenaltyAreaDepth()) this.field.setPenaltyLength(packet.getGeometry().getField().getPenaltyAreaDepth());
            if (packet.getGeometry().getField().hasGoalDepth()) this.field.setGoalLength(packet.getGeometry().getField().getGoalDepth());
            if (packet.getGeometry().getField().hasGoalWidth()) this.field.setGoalWidth(packet.getGeometry().getField().getGoalWidth());
            for(VisionGeometry.CameraCalibration cameraCalibration : packet.getGeometry().getCalibList()) {
                this.cameraCalibrationMap.put(cameraCalibration.getCameraId(), cameraCalibration);
            }

            for (VisionGeometry.FieldCircularArc arc : packet.getGeometry().getField().getFieldArcsList()) {
                if (arc.getName().equals("CenterCircle")) {
                    this.field.setCenterCircleRadius(arc.getRadius());
                }
            }

            for (VisionGeometry.FieldLineSegment line : packet.getGeometry().getField().getFieldLinesList()) {
                if (line.getName().equals("LeftPenaltyStretch")) {
                    this.field.setPenaltyWidth(line.getP2().getY() - line.getP1().getY());
                } else if(line.getName().equals("LeftFieldLeftPenaltyStretch")) {
                    this.field.setPenaltyLength(line.getP2().getX() - line.getP1().getX());
                }
            }
        }
        if (ConfigManager.getInstance().getConfig().demo.enable) {
            switch (ConfigManager.getInstance().getConfig().demo.difficulty) {
                case 0:
                    this.field.setPenaltyLength(this.field.getGameWidth() / 2);
                    break;
                case 1:
                    this.field.setPenaltyLength(this.field.getGameWidth() / 4);
                    break;
                case 2:
                    this.field.setPenaltyLength(0);
                    break;
            }
            this.field.setPenaltyWidth(this.field.getFieldHeight());
        }
        if (ConfigManager.getInstance().getConfig().useTracker) {
            return;
        }
        // update ball
        this.updaterBall.update(packet.getDetection(), this.cameraCalibrationMap);

        // update robot
        for (VisionDetection.Robot robot : packet.getDetection().getRobotsBlueList()) {
            int id = robot.getRobotId();
            if (!this.updaterRobotMap.containsKey(TeamColor.BLUE, id)) {
                this.updaterRobotMap.put(TeamColor.BLUE, id, new UpdaterRobot(TeamColor.BLUE, id));
            }
        }
        for (VisionDetection.Robot robot : packet.getDetection().getRobotsYellowList()) {
            int id = robot.getRobotId();
            if (!this.updaterRobotMap.containsKey(TeamColor.YELLOW, id)) {
                this.updaterRobotMap.put(TeamColor.YELLOW, id, new UpdaterRobot(TeamColor.YELLOW, id));
            }
        }

        for (UpdaterRobot ur : this.updaterRobotMap.values()) {
            ur.update(packet.getDetection());
            if (!this.prevVisionDetected) {
                if (ur.filterManual.isPresent()) {
                    ur.filterManual.get().updateObserver(0.0, 0.0, 0.0);
                }
            }
        }
        this.prevVisionDetected = true;
    }

    synchronized public void updateVisionTracker(@Nullable VisionWrapperTracked.TrackerWrapperPacket packet, int perSec) {
        this.trackerPerSec = perSec;
        if (!ConfigManager.getInstance().getConfig().useTracker) {
            return;
        }
        if (packet == null) {
            for (UpdaterRobot ur : this.updaterRobotMap.values()) {
                ur.update((VisionDetectionTracked.TrackedFrame) null);
            }
            return;
        }
        if (packet.hasTrackedFrame()) {
            this.updaterBall.update(packet.getTrackedFrame());
            // update robot
            for (VisionDetectionTracked.TrackedRobot robot : packet.getTrackedFrame().getRobotsList()) {
                if (robot.getRobotId().getTeam() == GcCommon.Team.BLUE) {
                    int id = robot.getRobotId().getId();
                    if (!this.updaterRobotMap.containsKey(TeamColor.BLUE, id)) {
                        this.updaterRobotMap.put(TeamColor.BLUE, id, new UpdaterRobot(TeamColor.BLUE, id));
                    }
                } else if (robot.getRobotId().getTeam() == GcCommon.Team.YELLOW) {
                    int id = robot.getRobotId().getId();
                    if (!this.updaterRobotMap.containsKey(TeamColor.YELLOW, id)) {
                        this.updaterRobotMap.put(TeamColor.YELLOW, id, new UpdaterRobot(TeamColor.YELLOW, id));
                    }
                }
            }
            for (UpdaterRobot ur : this.updaterRobotMap.values()) {
                ur.update(packet.getTrackedFrame());
            }
        }
    }

    /**
     * FilterControlledRobot用のSendCommandの更新
     * @param color 色
     * @param id id
     * @param command 送信済みコマンド
     */
    synchronized public void updateSendCommand(@Nonnull TeamColor color, int id, SendCommand command) {
        if (command.isDirect()) {
            if (this.updaterRobotMap.containsKey(color, id)) {
                FilteredRobot robot = this.updaterRobotMap.get(color, id).getValue();
                if (!robot.isLost()) {
                    double st = FastMath.sin(robot.getTheta());
                    double ct = FastMath.cos(robot.getTheta());
                    double vxf = ct * command.getVx() - st * command.getVy();
                    double vyf = st * command.getVx() + ct * command.getVy();
                    double omega = command.getOmega();
                    if (this.updaterRobotMap.get(color, id).filterManual.isPresent()) {
                        this.updaterRobotMap.get(color, id).filterManual.get().updateObserver(vxf, vyf, omega);
                    }
                    if (this.updaterRobotMap.get(color, id).filterSame.isPresent() && this.updaterRobotMap.get(color, id).filterSame.get() instanceof IObserver) {
                        ((IObserver) this.updaterRobotMap.get(color, id).filterSame.get()).updateObserver(vxf, vyf, omega);
                    }
                }
            }
            return;
        }
        Optional<FilteredRobot> robotOptional = Optional.empty();
        if (this.updaterRobotMap.containsKey(color, id)) {
            robotOptional = Optional.of(this.updaterRobotMap.get(color, id).getValue());
        }
        if (robotOptional.isPresent() && !robotOptional.get().isLost()) {
            FilteredRobot robot = robotOptional.get();
            double st = FastMath.sin(robot.getTheta());
            double ct = FastMath.cos(robot.getTheta());
            double vxf = ct * command.getVx() - st * command.getVy();
            double vyf = st * command.getVx() + ct * command.getVy();
            double omega = command.getOmega();

            if (this.updaterRobotMap.containsKey(color, id)) {
                if (this.updaterRobotMap.get(color, id).filterManual.isPresent()) {
                    this.updaterRobotMap.get(color, id).filterManual.get().updateObserver(vxf, vyf, omega);
                }
                if (this.updaterRobotMap.get(color, id).filterSame.isPresent() && this.updaterRobotMap.get(color, id).filterSame.get() instanceof IObserver) {
                    ((IObserver) this.updaterRobotMap.get(color, id).filterSame.get()).updateObserver(vxf, vyf, omega);
                }
            }
        }
    }

    synchronized public void clearAllFilterRobot(@Nonnull TeamColor color, int id) {
        if (!this.updaterRobotMap.containsKey(color, id)) {
            this.updaterRobotMap.put(color, id, new UpdaterRobot(color, id));
        }
        this.updaterRobotMap.get(color, id).clearAllFilters();
    }

    synchronized public void clearFilterRobot(@Nonnull TeamColor color, int id) {
        if (!this.updaterRobotMap.containsKey(color, id)) {
            this.updaterRobotMap.put(color, id, new UpdaterRobot(color, id));
        }
        this.updaterRobotMap.get(color, id).clearFilters();
    }
    /**
     * コンフィグが変更されたタイミングで呼び出す
     * @param color
     * @param id
     * @param filter
     */
    synchronized public void setFilterRobot(@Nonnull TeamColor color, int id, AbstractFilter<FilteredRobot, RawRobot> filter) {
        if (!this.updaterRobotMap.containsKey(color, id)) {
            this.updaterRobotMap.put(color, id, new UpdaterRobot(color, id));
        }
        this.updaterRobotMap.get(color, id).clearFilters();
        this.logger.debug("clear filter and set {} : [{}, {}]", filter.getClass().getName(), color, id);
        if (filter instanceof AbstractFilterSame<FilteredRobot, RawRobot>) {
            this.updaterRobotMap.get(color, id).setFilterSame((AbstractFilterSame<FilteredRobot, RawRobot>) filter);
        } else if (filter instanceof AbstractFilterManual<FilteredRobot, RawRobot>) {
            this.updaterRobotMap.get(color, id).setFilterManual((AbstractFilterManual<FilteredRobot, RawRobot>) filter);
            if (this.updaterRobotMap.get(color, id).filterManual.isPresent()) {
                this.updaterRobotMap.get(color, id).filterManual.get().updateObserver(0.0, 0.0, 0.0);
            }
        }
    }

    synchronized public void setController(@Nonnull TeamColor color, int id, @Nonnull AbstractController controller) {
        this.logger.debug("set controller {} : [{}, {}]", controller.getClass().getName(), color, id);
        this.controllerMap.put(color, id, controller);
    }

    synchronized public void setControllerMode(boolean isController) {
        this.isController = isController;
    }

    synchronized public void clearAllRobotMap(TeamColor color) {
        this.controllerMap.getMap(color).clear();
        this.updaterRobotMap.getMap(color).clear();
        this.commandMap.getMap(color).clear();
        this.roleMap.getMap(color).clear();
    }

    synchronized public void setFilterBall(AbstractFilter<FilteredBall, RawBall> filter) {
        this.updaterBall.clearFilters();
        if (filter instanceof AbstractFilterSame<FilteredBall, RawBall>) {
            this.updaterBall.setFilterSame((AbstractFilterSame<FilteredBall, RawBall>) filter);
        } else if(filter instanceof AbstractFilterManual<FilteredBall, RawBall>) {
            this.updaterBall.setFilterManual((AbstractFilterManual<FilteredBall, RawBall>) filter);
        }
    }

    synchronized public void clearFilterBall() {
        this.updaterBall.clearFilters();
    }

    /**
     * コマンドを更新 GameThreadから呼び出されることを想定
     * @param commands 送信するコマンドのリスト フィールド基準
     */
    synchronized public void updateCommands(MapLikeRobotList<Command> commands) {
        this.commandMap.putAll(commands);
    }

    synchronized public void updateActions(MapLikeRobotList<AbstractAction> actionMap) {
        this.actionMap.clear();
        this.actionMap.putAll(actionMap);
    }

    synchronized public void updateRoles(MapLikeRobotList<AbstractRole> roleMap) {
        this.roleMap.clear();
        this.roleMap.putAll(roleMap);
    }

    /**
     * コマンドを更新 GameThreadから呼び出されることを想定
     * @param commands 送信するコマンドのリスト フィールド基準
     */
    synchronized public void updateCommands(Map<Integer, Command> commands, TeamColor color) {
        this.commandMap.putAll(color, commands);
    }

    synchronized public void updateActions(Map<Integer, AbstractAction> actionMap, TeamColor color) {
        this.actionMap.getMap(color).clear();
        this.actionMap.putAll(color, actionMap);
    }

    synchronized public void updateRoles(Map<Integer, AbstractRole> roleMap, TeamColor color) {
        this.roleMap.getMap(color).clear();
        this.roleMap.putAll(color, roleMap);
    }

    /**
     * ゲームルールによる移動速度制限
     * @param limit ルール規定の移動速度
     */
    synchronized public void setVelocityLimit(double limit) {
        for (AbstractController c : this.controllerMap.values()) {
            c.setVelocityLimit(limit);
        }
    }

    synchronized public void setHaveBall(boolean haveBall, @Nonnull TeamColor color) {
        if (color.isYellow()) {
            if (!this.yellowHaveBall) {
                this.yellowDribbleStartPos = updaterBall.getValue().position();
            }
            this.yellowHaveBall = haveBall;
        } else {
            if (!this.blueHaveBall) {
                this.blueDribbleStartPos = updaterBall.getValue().position();
            }
            this.blueHaveBall = haveBall;
        }
    }

    synchronized public void setReplayWorld(@Nonnull World replayWorld) {
        this.replayWorld = Optional.of(replayWorld);
    }

    synchronized public void replayStop() {
        this.replayWorld = Optional.empty();
    }

    /* ========================= 以下 getter =========================*/

    synchronized public long getCommonCycle() {
        return this.commonCycle;
    }

    synchronized public double getCommonTime() {
        return (double)this.commonCycle * ConfigManager.getInstance().getConfig().getCycleTime();
    }

    synchronized public boolean getControllerMode() {
        return this.isController;
    }

    synchronized public Field getField() {
        if(this.replayWorld.isPresent()) return this.replayWorld.get().getField();
        return this.field.clone();
    }

    synchronized public int getVisionPerSec() {
        return this.visionPerSec;
    }

    synchronized public int getTrackerPerSec() {
        return this.trackerPerSec;
    }

    synchronized public FilteredBall getBall() {
        return this.updaterBall.getValue();
    }

    synchronized public boolean isBallOutSide() {
        return this.updaterBall.isOutSide();
    }

    @Nonnull
    synchronized public WrapperCloneableMap<Integer, FilteredRobot> getFilteredRobots(TeamColor color) {
        Map<Integer, FilteredRobot> map = new HashMap<>();
        for (Map.Entry<Integer, UpdaterRobot> entry : this.updaterRobotMap.entrySet(color)) {
            if (entry.getValue().isVisible()) {
                map.put(entry.getKey(), entry.getValue().getValue());
            }
        }
        return new WrapperCloneableMap<>(map);
    }

    @Nonnull
    synchronized public WrapperCloneableMap<Integer, IntegratedRobot> getIntegratedMap(TeamColor color) {
        return this.getIntegratedMapPrivate(color);
    }

    @Nonnull
    private WrapperCloneableMap<Integer, IntegratedRobot> getIntegratedMapPrivate(TeamColor color) {
        Map<Integer, IntegratedRobot> map = new HashMap<>();
        for (Map.Entry<Integer, UpdaterRobot> entry : this.updaterRobotMap.entrySet(color)) {
            if (entry.getValue().isVisible()) {
                map.put(entry.getKey(), new IntegratedRobot(entry.getKey(), color,
                        entry.getValue().getValue(), this.commandMap.containsKey(color, entry.getKey()) ? Optional.of(this.commandMap.get(color, entry.getKey()).clone())
                        : Optional.empty(), this.actionMap.containsKey(color, entry.getKey()) ? Optional.of(this.actionMap.get(color, entry.getKey()))
                        : Optional.empty(), this.roleMap.containsKey(color, entry.getKey()) ? Optional.of(this.roleMap.get(color, entry.getKey()))
                        : Optional.empty()));
            }
        }
        return new WrapperCloneableMap<>(map);
    }

    synchronized public World getWorld(boolean invert) {
        if(this.replayWorld.isPresent())
            return invert ? this.replayWorld.get().getInverted() : this.replayWorld.get();
        World world = new World(this.getIntegratedMapPrivate(TeamColor.BLUE), this.getIntegratedMapPrivate(TeamColor.YELLOW),
                this.updaterBall.getValue(), this.field.clone(), this.blueDribbleStartPos, this.yellowDribbleStartPos, this.blueHaveBall, this.yellowHaveBall);
        return invert ? world.getInverted() : world;
    }

    /**
     * CommandがUpdateされてから一度のみ有効
     * 登録されたコマンドをコントローラに通す
     * @return ロボット基準に変換されたコマンド
     */
    @Nonnull
    synchronized public WrapperCloneableList<SendCommand> makeSendCommands() {
        List<SendCommand> result = new ArrayList<>();
        for (Map.Entry<Pair<TeamColor, Integer>, Command> entry : this.commandMap.entrySet()) {
            Command command = entry.getValue();
            TeamColor color = entry.getKey().getKey();
            int id = entry.getKey().getValue();
            if (!command.wasSent()) {
                if (command.isHalt()) {
                    result.add(RobotDriver.makeHaltCommand(id, color));
                } else if (command.isDirect()) {
                    result.add(new SendCommand(id, color, command.getKickFlag(), command.getDribble(), command.getTargetVel().getX(),
                            command.getTargetVel().getY(), command.getTargetOmega(), true));
                } else if (this.controllerMap.containsKey(color, id) && this.updaterRobotMap.containsKey(color, id)) {
                    Vector3D vector3D = this.controllerMap.get(color, id).update(this.updaterRobotMap.get(color, id).getValue().clone(),
                            this.field, command);
                    result.add(new SendCommand(entry.getKey().getValue(), entry.getKey().getKey(), entry.getValue().getKickFlag(),
                            entry.getValue().getDribble(), vector3D.getX(), vector3D.getY(), vector3D.getZ(), false));
                } else if (this.updaterRobotMap.containsKey(color, id)) {
                    this.logger.warn("No controller is set : [{}, {}]", color, id);
                    this.logger.info("Requesting reset....");
                    ConfigManager.getInstance().setNeedReset(color,true);
                }
                command.setSent(true);
            }
        }
        return new WrapperCloneableList<>(result);
    }
}
