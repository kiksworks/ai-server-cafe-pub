package ai_server_cafe.updater;

import ai_server_cafe.model.field.FilteredBall;
import ai_server_cafe.model.field.RawBall;
import ai_server_cafe.network.proto.ssl.vision.VisionDetection;
import ai_server_cafe.network.proto.ssl.vision.VisionDetectionTracked;
import ai_server_cafe.network.proto.ssl.vision.VisionGeometry;
import ai_server_cafe.util.TimeHelper;
import ai_server_cafe.util.game.FilterHelper;
import ai_server_cafe.util.interfaces.IFuncParam2;
import ai_server_cafe.util.interfaces.InterfaceHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;


public class UpdaterBall extends AbstractFilteredUpdater<FilteredBall, RawBall> {
    // 各カメラで検出されたボールの生データ
    private final Map<Integer, VisionDetection.Ball> rawBallMap;
    private boolean isOutSide;
    private double outSideCount;

    public UpdaterBall() {
        super(new FilteredBall());
        this.rawBallMap = new HashMap<>();
        this.value.setLost(true);
        this.isOutSide = false;
        this.outSideCount = 0;
    }

    synchronized public FilteredBall getValue() {
        if (ConfigManager.getInstance().getConfig().filterConfig.useBallEstimator && !this.value.isLost()) {
            double now = TimeHelper.now();
            if (this.value.getStateAfter(now - this.updateTime).isPresent()) {
                FilteredBall result = this.value.getStateAfter(now - this.updateTime).get().clone();
                result.setLost(false);
                return result;
            }
        }
        return super.getValue();
    }

    synchronized public void update(@Nonnull VisionDetection.Frame detection, @Nonnull Map<Integer, VisionGeometry.CameraCalibration> cameraCalibrationMap) {
        final int cameraId = detection.getCameraId();
        final double captureTime = detection.getTCapture();
        // 検出されたボールの中から, 最も前回と近い値を選択候補に登録する
        // FIXME:
        // 現在の実装は, フィールドにボールが1つしかないと仮定している
        // 1つのカメラで複数のボールが検出された場合, 意図しないデータが選択される可能性がある
        FilteredBall ballNext = this.value.getStateAfter(0.016).orElseGet(new Supplier<FilteredBall>() {
            @Override
            public FilteredBall get() {
                return UpdaterBall.this.value;
            }
        });
        List<VisionDetection.Ball> balls = detection.getBallsList();
        Optional<VisionDetection.Ball> candidate = balls.stream().min(InterfaceHelper.getComparator(
                new IFuncParam2<Boolean, VisionDetection.Ball, VisionDetection.Ball>() {
                    @Override
                    public Boolean function(VisionDetection.Ball ball, VisionDetection.Ball ball2) {
                        return MathHelper.distance2D(ball, ballNext) < MathHelper.distance2D(ball2, ballNext);
                    }
        }));
        if (candidate.isPresent()) {
            this.rawBallMap.put(cameraId, candidate.get());
        } else {
            this.rawBallMap.remove(cameraId);
        }
        // 候補の中から, 最も前回と近いボールを求める
        Optional<Pair<Integer, VisionDetection.Ball>> reliable = InterfaceHelper.makePairList(this.rawBallMap).stream().min(
                InterfaceHelper.getComparator(new IFuncParam2<Boolean, Pair<Integer, VisionDetection.Ball>, Pair<Integer, VisionDetection.Ball>>() {
                    @Override
                    public Boolean function(Pair<Integer, VisionDetection.Ball> integerBallPair, Pair<Integer, VisionDetection.Ball> integerBallPair2) {
                        return MathHelper.distance2D(integerBallPair.getValue(), ballNext) < MathHelper.distance2D(integerBallPair2.getValue(), ballNext);
                    }
                }));
        if (reliable.isPresent()) {
            int oCId = reliable.get().getKey();
            if (oCId == cameraId) {
                FilteredBall rawFb = new FilteredBall();
                rawFb.setX(reliable.get().getValue().getX());
                rawFb.setY(reliable.get().getValue().getY());
                rawFb.setZ(reliable.get().getValue().getZ());
                rawFb.setHasZ(false);
                if (!FilterHelper.isOverOutSide(rawFb, ConfigManager.getInstance().getConfig())) {
                    if (cameraCalibrationMap.containsKey(oCId)) {
                        VisionGeometry.CameraCalibration cameraCalibration = cameraCalibrationMap.get(oCId);
                        if (cameraCalibration.hasDerivedCameraWorldTx() && cameraCalibration.hasDerivedCameraWorldTy()
                                && cameraCalibration.hasDerivedCameraWorldTz()) {
                            double cameraX = cameraCalibration.getDerivedCameraWorldTx();
                            double cameraY = cameraCalibration.getDerivedCameraWorldTy();
                            double cameraZ = cameraCalibration.getDerivedCameraWorldTz();
                            rawFb.setCameraPos(cameraX, cameraY, cameraZ);
                        }
                    }
                }
                if (this.filterSame.isPresent()) {
                    // filterSame が設定されていたらFilterを通した値を使う
                    Optional<FilteredBall> fb = this.filterSame.get().updateRaw(Optional.of(rawFb.getRaw()), captureTime);
                    if (fb.isPresent()) {
                        this.value = fb.get();
                        this.value.setLost(false);
                    } else {
                        this.value.setLost(true);
                    }
                    this.updateTime = TimeHelper.now();
                } else if (this.filterManual.isPresent()) {
                    // filterManual が設定されていたら観測値を通知する
                    this.filterManual.get().updateRaw(Optional.of(rawFb.getRaw()), captureTime);
                } else {
                    // Filterが登録されていない場合はそのままの値を使う
                    this.value = rawFb;
                    this.value.setLost(false);
                    this.updateTime = TimeHelper.now();
                }
            }
        } else if(this.filterSame.isPresent()) {
            Optional<FilteredBall> ofb = this.filterSame.get().updateRaw(Optional.empty(), captureTime);
            if (ofb.isPresent()) {
                this.value = ofb.get();
                // Ballはロストしても予測値かどうかにかかわらずフィールドから消えないのでロストを通知する必要がある
                this.value.setLost(true);
            } else {
                this.value.setLost(true);
            }
            this.updateTime = TimeHelper.now();
        } else if(this.filterManual.isPresent()) {
            this.filterManual.get().updateRaw(Optional.empty(), captureTime);
        } else {
            this.value.setLost(true);
            this.updateTime = TimeHelper.now();
        }

        if(FastMath.log(this.value.position().getNormSq() + 1) > 18 || this.value.isLost()) {
            // フィールド外に遠くまで飛ぶか、
            // 座標だけ残ったままフィールド外に飛んだ場合カウントする("Locate ball here"で誤審するのを防ぐため)
            this.outSideCount++;
            this.outSideCount = FastMath.min(this.outSideCount, 2 / ConfigManager.getInstance().getCycleTime());
        } else {
            this.outSideCount = 0;
            this.isOutSide = false;
        }

        if(this.outSideCount > 1 / ConfigManager.getInstance().getCycleTime()) {
            this.isOutSide = true;
        }
    }

    synchronized public void update(@Nonnull VisionDetectionTracked.TrackedFrame packet) {
        final double capturedTime = packet.getTimestamp();
        if (!packet.getBallsList().isEmpty() && (!packet.getBallsList().getFirst().hasVisibility()
                || packet.getBallsList().getFirst().getVisibility() > 0.5)) {
            // リストの最初の要素は primary ball
            VisionDetectionTracked.TrackedBall rawBall = packet.getBallsList().getFirst();
            FilteredBall rawFb = new FilteredBall();
            rawFb.setX(1000.0 * rawBall.getPos().getX());
            rawFb.setY(1000.0 * rawBall.getPos().getY());
            rawFb.setZ(1000.0 * rawBall.getPos().getZ());
            if (rawBall.hasVel()) {
                rawFb.setVx(1000.0 * rawBall.getVel().getX());
                rawFb.setVy(1000.0 * rawBall.getVel().getY());
                rawFb.setVz(1000.0 * rawBall.getVel().getZ());
            }
            if (this.filterSame.isPresent()) {
                // filter_same_が設定されていたらFilterを通した値を使う
                Optional<FilteredBall> fb = this.filterSame.get().updateRaw(Optional.of(rawFb.getRaw()), capturedTime);
                if (fb.isPresent()) {
                    this.value = fb.get();
                    this.value.setLost(false);
                } else {
                    this.value.setLost(true);
                }
            } else if (this.filterManual.isPresent()) {
                // filter_manual_が設定されていたら観測値を通知する
                this.filterManual.get().updateRaw(Optional.of(rawFb.getRaw()), capturedTime);
            } else {
                // Filterが登録されていない場合はそのままの値を使う
                this.value = rawFb;
                this.value.setLost(false);
            }
        } else {
            // Filter が設定されていたらロストしたことを通知する
            if (this.filterSame.isPresent()) {
                Optional<FilteredBall> fb = this.filterSame.get().updateRaw(Optional.empty(), capturedTime);
                if (fb.isPresent()) {
                    this.value = fb.get();
                    this.value.setLost(true);
                } else {
                    this.value.setLost(true);
                }
            } else if (this.filterManual.isPresent()) {
                this.filterManual.get().updateRaw(Optional.empty(), capturedTime);
            } else {
                this.value.setLost(true);
            }
        }
    }

    synchronized public boolean isOutSide() {
        return this.isOutSide;
    }
}
