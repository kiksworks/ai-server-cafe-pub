package ai_server_cafe.updater;

import ai_server_cafe.config.Config;
import ai_server_cafe.filter.limited.FilterUncontrolledRobot;
import ai_server_cafe.model.field.FilteredRobot;
import ai_server_cafe.model.field.RawRobot;
import ai_server_cafe.network.proto.ssl.gc.GcCommon;
import ai_server_cafe.network.proto.ssl.vision.VisionDetection;
import ai_server_cafe.network.proto.ssl.vision.VisionDetectionTracked;
import ai_server_cafe.util.TeamColor;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.interfaces.IFuncParam1;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 *  UpdaterWorldからのアクセスのみOK
 */
public class UpdaterRobot extends AbstractFilteredUpdater<FilteredRobot, RawRobot> {
    private final TeamColor color;
    private final int id;
    private Map<Integer, VisionDetection.Robot> rawRobotMap;
    private final Logger logger;

    public UpdaterRobot(@Nonnull TeamColor color, int id) {
        super(new FilteredRobot());
        this.color = color;
        this.id = id;
        this.rawRobotMap = new HashMap<>();
        this.value.setLost(true);
        this.setDefaultFilter(FilterUncontrolledRobot.class, ConfigManager.getInstance().getConfig().filterConfig.lostDuration);
        this.logger = LogManager.getLogger("robot_updater::" + color.name() + id);;
    }

    synchronized public boolean isVisible() {
        return !this.value.isLost();
    }

    public int getId() {
        return this.id;
    }

    public TeamColor getColor() {
        return this.color;
    }

    synchronized public FilteredRobot getValue() {
        Config.Filter filterConfig = ConfigManager.getInstance().getConfig().filterConfig;
        if (filterConfig.useFreeRobotEstimator && this.filterSame.isPresent() && !this.value.isLost()) {
            double now = TimeHelper.now();
            if (this.value.getStateAfter(now - this.updateTime).isPresent()) {
                FilteredRobot robot = this.value.getStateAfter(now - this.updateTime).get().clone();
                robot.setLost(this.value.isLost());
                return robot;
            }
        }
        if (filterConfig.useControlledRobotEstimator && this.filterManual.isPresent() && !this.value.isLost()) {
            double now = TimeHelper.now();
            if (this.value.getStateAfter(now - this.updateTime).isPresent()) {
                FilteredRobot robot = this.value.getStateAfter(now - this.updateTime).get().clone();
                robot.setLost(this.value.isLost());
                return robot;
            }
        }
        return super.getValue();
    }

    synchronized public void update(@Nullable VisionDetection.Frame detection) {
        if (detection == null) {
            // ロスト
            if (this.filterSame.isPresent()) {
                Optional<FilteredRobot> ofr = this.filterSame.get().updateRaw(Optional.empty(), TimeHelper.now());
                if (ofr.isPresent()) {
                    this.value = ofr.get();
                    this.value.setLost(false);
                } else {
                    this.value.setLost(true);
                }
                this.updateTime = TimeHelper.now();
            } else if(this.filterManual.isPresent()) {
                this.filterManual.get().updateRaw(Optional.empty(), TimeHelper.now());
            } else {
                this.value.setLost(true);
                this.updateTime = TimeHelper.now();
            }
            return;
        }
        // カメラID
        int cameraId = detection.getCameraId();
        // キャプチャされた時間
        double capturedTime = detection.getTCapture();

        // 保持している生データを更新する
        Optional<VisionDetection.Robot> rawRobotOpt = Optional.empty();
        if (this.color.isYellow()) {
            rawRobotOpt = detection.getRobotsYellowList().stream().filter(InterfaceHelper.getPredicate(new IFuncParam1<Boolean, VisionDetection.Robot>() {
                @Override
                public Boolean function(VisionDetection.Robot robot) {
                    return robot.getRobotId() == UpdaterRobot.this.id;
                }
            })).findAny();
        } else {
            rawRobotOpt = detection.getRobotsBlueList().stream().filter(InterfaceHelper.getPredicate(new IFuncParam1<Boolean, VisionDetection.Robot>() {
                @Override
                public Boolean function(VisionDetection.Robot robot) {
                    return robot.getRobotId() == UpdaterRobot.this.id;
                }
            })).findAny();
        }

        if (rawRobotOpt.isPresent()) {
            this.rawRobotMap.put(cameraId, rawRobotOpt.get());
        } else {
            this.rawRobotMap.remove(cameraId);
        }
        // 最もconfidenceの高い要素を選択して値の更新を行う
        Optional<Pair<Integer, VisionDetection.Robot>> reliableOpt = InterfaceHelper.makePairList(this.rawRobotMap).stream().max(InterfaceHelper.getComparator(new IFuncParam1<Double, Pair<Integer, VisionDetection.Robot>>() {
            @Override
            public Double function(Pair<Integer, VisionDetection.Robot> integerRobotPair) {
                return (double) integerRobotPair.getValue().getConfidence();
            }
        }));
        if (reliableOpt.isPresent() && this.rawRobotMap.get(reliableOpt.get().getKey()).getConfidence() >= ConfigManager.getInstance().getConfig().filterConfig.robotConfidenceThreshold) {
            Pair<Integer, VisionDetection.Robot> pair = reliableOpt.get();
            // カメラIDが一致していたら値の更新を行う
            // (現在のカメラで新たに検出された or
            // 現在のカメラで検出された値のほうがconfidenceが高かった)
            if (pair.getKey() == cameraId) {
                FilteredRobot rawValue = new FilteredRobot();
                rawValue.setX(pair.getValue().getX());
                rawValue.setY(pair.getValue().getY());
                rawValue.setTheta(pair.getValue().getOrientation());

                // 2つのFilterが設定されておらず, かつfilter_initializer_が設定されていたら
                // filter_initializer_でFilterを初期化する
                if (this.initializeFilterFunc.isPresent() && !this.filterSame.isPresent() && !this.filterManual.isPresent()) {
                    this.filterSame = Optional.of(this.initializeFilterFunc.get().function());
                }

                if (this.filterSame.isPresent()) {
                    Optional<FilteredRobot> ofr = this.filterSame.get().updateRaw(Optional.of(rawValue.getRaw()), capturedTime);
                    if (ofr.isPresent()) {
                        this.value = ofr.get();
                        this.value.setLost(false);
                    } else {
                        this.value.setLost(true);
                    }
                    this.updateTime = TimeHelper.now();
                } else if(this.filterManual.isPresent()) {
                    this.filterManual.get().updateRaw(Optional.of(rawValue.getRaw()), capturedTime);
                } else {
                    this.value = rawValue;
                    this.value.setLost(false);
                    this.updateTime = TimeHelper.now();
                }
            }
            // カメラIDが一致しないときは更新しない
            // (現在のカメラで検出されたがconfidenceが低かった or 現在のカメラで検出されなかった)
        } else {
            // ロスト
            if (this.filterSame.isPresent()) {
                Optional<FilteredRobot> ofr = this.filterSame.get().updateRaw(Optional.empty(), capturedTime);
                if (ofr.isPresent()) {
                    this.value = ofr.get();
                    this.value.setLost(false);
                } else {
                    this.value.setLost(true);
                }
                this.updateTime = TimeHelper.now();
            } else if(this.filterManual.isPresent()) {
                this.filterManual.get().updateRaw(Optional.empty(), capturedTime);
            } else {
                this.value.setLost(true);
                this.updateTime = TimeHelper.now();
            }
        }
    }

    synchronized public void update(@Nullable VisionDetectionTracked.TrackedFrame detection) {
        if (detection == null) {
            // ロスト
            if (this.filterSame.isPresent()) {
                Optional<FilteredRobot> ofr = this.filterSame.get().updateRaw(Optional.empty(), TimeHelper.now());
                if (ofr.isPresent()) {
                    this.value = ofr.get();
                    this.value.setLost(false);
                } else {
                    this.value.setLost(true);
                }
                this.updateTime = TimeHelper.now();
            } else if(this.filterManual.isPresent()) {
                this.filterManual.get().updateRaw(Optional.empty(), TimeHelper.now());
            } else {
                this.value.setLost(true);
                this.updateTime = TimeHelper.now();
            }
            return;
        }
        final double capturedTime = detection.getTimestamp();
        Optional<VisionDetectionTracked.TrackedRobot> optTracked = detection.getRobotsList().stream().filter(InterfaceHelper.getPredicate(
                new IFuncParam1<Boolean, VisionDetectionTracked.TrackedRobot>() {
                    @Override
                    public Boolean function(VisionDetectionTracked.TrackedRobot trackedRobot) {
                        return trackedRobot.getRobotId().getId() == id &&
                                (!color.isYellow() ? trackedRobot.getRobotId().getTeam() == GcCommon.Team.BLUE
                                        : trackedRobot.getRobotId().getTeam() == GcCommon.Team.YELLOW);
                    }
                })).findAny();
        if (optTracked.isPresent() && (!optTracked.get().hasVisibility()
                || optTracked.get().getVisibility() > ConfigManager.getInstance().getConfig().filterConfig.robotConfidenceThreshold)) {
            FilteredRobot fr = new FilteredRobot();
            fr.setX(1000.0 * optTracked.get().getPos().getX());
            fr.setY(1000.0 * optTracked.get().getPos().getY());
            fr.setTheta(optTracked.get().getOrientation());
            if (optTracked.get().hasVel()) {
                fr.setVx(1000.0 * optTracked.get().getVel().getX());
                fr.setVy(1000.0 * optTracked.get().getVel().getY());
                fr.setOmega(optTracked.get().getVelAngular());
            }
            if (this.initializeFilterFunc.isPresent() && !this.filterSame.isPresent() && !this.filterManual.isPresent()) {
                this.filterSame = Optional.of(this.initializeFilterFunc.get().function());
            }

            if (this.filterSame.isPresent()) {
                Optional<FilteredRobot> ofr = this.filterSame.get().updateRaw(Optional.of(fr.getRaw()), capturedTime);
                if (ofr.isPresent()) {
                    this.value = ofr.get();
                    this.value.setLost(false);
                } else {
                    this.value.setLost(true);
                }
                this.updateTime = TimeHelper.now();
            } else if(this.filterManual.isPresent()) {
                this.filterManual.get().updateRaw(Optional.of(fr.getRaw()), capturedTime);
            } else {
                this.value = fr;
                this.value.setLost(false);
                this.updateTime = TimeHelper.now();
            }
        } else {
            // ロスト
            if (this.filterSame.isPresent()) {
                Optional<FilteredRobot> ofr = this.filterSame.get().updateRaw(Optional.empty(), capturedTime);
                if (ofr.isPresent()) {
                    this.value = ofr.get();
                    this.value.setLost(false);
                } else {
                    this.value.setLost(true);
                }
                this.updateTime = TimeHelper.now();
            } else if(this.filterManual.isPresent()) {
                this.filterManual.get().updateRaw(Optional.empty(), capturedTime);
            } else {
                this.value.setLost(true);
                this.updateTime = TimeHelper.now();
            }
        }
    }
}
