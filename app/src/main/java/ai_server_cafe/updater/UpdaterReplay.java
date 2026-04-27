package ai_server_cafe.updater;

import ai_server_cafe.records.AbstractRecordData;
import ai_server_cafe.records.ver0.RecordData0;
import ai_server_cafe.records.ver1.RecordData1;
import ai_server_cafe.replay.ReplayState;
import ai_server_cafe.replay.clip.ClipState;
import ai_server_cafe.replay.clip.FileExtension;
import ai_server_cafe.util.compressor.CompressHelper;
import ai_server_cafe.util.interfaces.IFunction;
import ai_server_cafe.util.interfaces.WrapperWeakCloneable;
import ai_server_cafe.util.reader.ReaderExternal;
import ai_server_cafe.util.writer.WriterExternal;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;

public final class UpdaterReplay {
    private static UpdaterReplay instance = null;
    private ReplayState replayState;
    private int format;
    private File file;
    private long replayFrame;
    private Optional<ReaderExternal> replayExternal;
    private long replayInitialFrame;
    private long replayLastFrame;
    private final Logger logger = LogManager.getLogger("updater-replay");
    private volatile int percent = 0;
    private boolean isLoaded;
    private IFunction<Void> loadedFunc;
    private ClipState clipState;
    private Optional<String> clipFileName;
    private FileExtension ext;
    private Optional<Long> clipInitialFrame;
    private Optional<Long> clipLastFrame;
    private long clipFrame;
    // ファイルを切り取る際、何行飛ばすか
    private long windowSize = 10L;
    private WriterExternal clipWriterExternal;
    // recordしたファイルのサイクル
    private double cycleTime = 1 / 60.0;

    private UpdaterReplay() {
        this.file = null;
        this.replayState = ReplayState.STOP;
        this.replayFrame = 0L;
        this.replayExternal = Optional.empty();
        this.replayInitialFrame = -1L;
        this.replayLastFrame = -1L;
        this.isLoaded = false;
        this.loadedFunc = null;
        this.clipState = ClipState.WAIT;
        this.clipFileName = Optional.empty();
        this.ext = FileExtension.CSV;
        this.clipFrame = 0L;
        this.clipInitialFrame = Optional.empty();
        this.clipLastFrame = Optional.empty();
        this.clipWriterExternal = null;
        this.format = -1;
    }

    public static synchronized UpdaterReplay getInstance() {
        if (instance == null) instance = new UpdaterReplay();
        return instance;
    }

    /**
     *
     * @return RecordDataで作れるかどうか
     */
    public synchronized boolean isPresent() {
        return UpdaterReplay.getInstance().getReplayFrameData(RecordData0.class).get().isPresent()
                || UpdaterReplay.getInstance().getReplayFrameData(RecordData1.class).get().isPresent();
    }

    /* ========================= 以下 getter =========================*/

    /**
     * @param clazz 再生するクラスを指定する
     * @return 現在のreplayFrameのRecordData
     */
    @Nonnull
    public synchronized <T extends AbstractRecordData>
        WrapperWeakCloneable<Optional<T>> getReplayFrameData(Class<? extends T> clazz) {
        Optional<T> result = Optional.empty();
        if (this.replayFrame >= this.replayLastFrame) {
            this.replayFrame = this.replayLastFrame;
        }
        if (this.replayState == ReplayState.STOP || this.replayExternal.isEmpty() || this.replayFrame - this.replayInitialFrame < 0)
            return new WrapperWeakCloneable<>(result);
        final Optional<String> readLine = this.replayExternal.get().readLine(this.replayFrame - this.replayInitialFrame);
        final Gson gson = new GsonBuilder().create();
        if (readLine.isPresent()) {
            try {
                final T t = gson.fromJson(CompressHelper.getDeCompressedString(readLine.get()), clazz);
                if (t.matchFormat()) {
                    result = Optional.of(t);
                }
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return new WrapperWeakCloneable<>(result);
    }

    /**
     *
     * @return 現在のclipFrameのCompressedString
     */
    @Nonnull
    public synchronized WrapperWeakCloneable<String> getClipFrameCompressedData() {
        if(this.clipInitialFrame.isEmpty() || this.clipLastFrame.isEmpty()) {
            return new WrapperWeakCloneable<>("");
        }
        if (this.clipFrame >= this.clipLastFrame.get()) {
            this.clipFrame = this.clipLastFrame.get();
        }
        if (this.clipState == ClipState.FINISH || this.replayExternal.isEmpty()
                || this.clipFrame - this.clipInitialFrame.get() < 0) {
            return new WrapperWeakCloneable<>("");
        }
        final Optional<String> readLine = this.replayExternal.get().readLine(this.clipFrame - this.clipInitialFrame.get());
        if (readLine.isPresent()) {
            return new WrapperWeakCloneable<>(readLine.get() + "\n");
        }
        return new WrapperWeakCloneable<>("");
    }

    /**
     * @param clazz 切り取るクラスを指定する
     * @return 現在のclipFrameのRecordData
     */
    @Nonnull
    public synchronized <T extends AbstractRecordData>
        WrapperWeakCloneable<Optional<T>> getClipFrameRecordData(Class<? extends T> clazz) {
        Optional<T> result = Optional.empty();
        if(this.clipInitialFrame.isEmpty() || this.clipLastFrame.isEmpty())
            return new WrapperWeakCloneable<>(result);
        if (this.clipFrame >= this.clipLastFrame.get())
            this.clipFrame = this.clipLastFrame.get();
        if (this.clipState == ClipState.FINISH || this.replayExternal.isEmpty()
                || this.clipFrame - this.clipInitialFrame.get() < 0)
            return new WrapperWeakCloneable<>(result);
        // bin -> RecordData
        final Optional<String> readLine = this.replayExternal.get().readLine(this.clipFrame - this.clipInitialFrame.get());
        final Gson gson = new GsonBuilder().create();
        if (readLine.isPresent()) {
            try {
                final T t = gson.fromJson(CompressHelper.getDeCompressedString(readLine.get()), clazz);
                if (t.matchFormat()) {
                    result = Optional.of(t);
                }
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        return new WrapperWeakCloneable<>(result);
    }

    /**
     *
     * @return 現在のReplayState
     */
    public synchronized ReplayState getReplayState() {
        return this.replayState;
    }

    /**
     *
     * @return 現在のClipState
     */
    public synchronized ClipState getClipState() {
        return this.clipState;
    }

    /**
     *
     * @return 切り取るファイルのInitialFrame
     */
    public synchronized WrapperWeakCloneable<Optional<Long>> getClipInitialFrame() {
        return new WrapperWeakCloneable<>(this.clipInitialFrame);
    }

    /**
     *
     * @return 切り取るファイルのLastFrame
     */
    public synchronized WrapperWeakCloneable<Optional<Long>> getClipLastFrame() {
        return new WrapperWeakCloneable<>(this.clipLastFrame);
    }

    /**
     *
     * @return 切り取るファイルで、何行飛ばしで切り取るか
     */
    public synchronized long getWindowSize() {
        return this.windowSize;
    }

    /**
     *
     * @return 開いているファイル
     */
    @Nonnull
    public synchronized WrapperWeakCloneable<Optional<File>> getExternalFile() {
        return new WrapperWeakCloneable<>((this.file != null && this.replayExternal.isPresent()) ?
                Optional.of(this.file) : Optional.empty());
    }

    /**
     *
     * @return 指定されたファイルを開けたかどうか
     */
    @Nonnull
    public synchronized WrapperWeakCloneable<Pair<File, Boolean>> getFile() {
        return new WrapperWeakCloneable<>(new Pair<>(this.file, this.isLoaded));
    }

    /**
     *
     * @return 現在のReplayFrame
     */
    public synchronized long getReplayFrame() {
        return this.replayFrame;
    }

    /**
     * @implNote デフォルト値-1
     */
    public synchronized long getReplayInitialFrame() {
        return this.replayInitialFrame;
    }

    /**
     * @implNote デフォルト値-1
     */
    public synchronized long getReplayLastFrame() {
        return this.replayLastFrame;
    }

    /**
     *
     * @return ファイル読み込みの進捗状況
     */
    public synchronized int getPercent() {
        return this.percent;
    }

    /**
     *
     * @return 開いているファイルのLastFrameをHH-mm-ssに変換する
     */
    public synchronized String getLastTime() {
        if(this.replayInitialFrame < 0L) return "0:00:00";
        final int replaySecond = (int) ((this.replayLastFrame - this.replayInitialFrame) * this.cycleTime);
        final int hour = replaySecond / 3600;
        final int minute = (replaySecond - (hour * 3600)) / 60;
        final int second = replaySecond % 60;
        final String hourStr = (hour != 0 ? String.valueOf(hour).concat(":") : "");
        final String minuteStr = (minute < 10 ? "0".concat(String.valueOf(minute)) : String.valueOf(minute));
        final String secondStr = (second < 10 ? "0".concat(String.valueOf(second)) : String.valueOf(second));
        return hourStr.concat(minuteStr).concat(":").concat(secondStr);
    }

    /**
     *
     * @return 現在のReplayFrameをHH-mm-ssに変換する
     */
    public synchronized String  getCurrentTime() {
        if(this.replayInitialFrame < 0L) return "0:00:00";
        final int currentSecond = (int) ((this.replayFrame - this.replayInitialFrame) * this.cycleTime);
        final int hour = currentSecond / 3600;
        final int minute = (currentSecond - (hour * 3600)) / 60;
        final int second = currentSecond % 60;
        final String hourStr = (hour != 0 ? String.valueOf(hour).concat(":") : "");
        final String minuteStr = (minute < 10 ? "0".concat(String.valueOf(minute)) : String.valueOf(minute));
        final String secondStr = (second < 10 ? "0".concat(String.valueOf(second)) : String.valueOf(second));
        return hourStr.concat(minuteStr).concat(":").concat(secondStr);
    }

    /**
     *
     * @return 書き込むファイル
     */
    public synchronized WrapperWeakCloneable<WriterExternal> getClipWriterExternal() {
        if(this.clipWriterExternal == null && this.clipInitialFrame.isPresent() && this.clipLastFrame.isPresent()) {
            String externalName;
            if(this.clipFileName.isPresent()) {
                externalName = this.clipFileName.get();
            } else {
                final int point = this.file.getName().lastIndexOf(".");
                String fileName = this.file.getName();
                if(point >= 0) {
                    fileName = this.file.getName().substring(0, point);
                }
                externalName = fileName + "_" + this.clipInitialFrame.get() + "-" + this.clipLastFrame.get() + "-v" + this.format;
            }
            externalName = externalName.concat(this.ext.getExtension());
            this.clipWriterExternal = new WriterExternal(externalName, this.logger, this.ext.getCharsets());
        }
        return new WrapperWeakCloneable<>(this.clipWriterExternal);
    }

    /**
     *
     * @return 開いているファイルのRecordDataのフォーマット
     */
    public synchronized int getFormat() {
        return this.format;
    }

    /**
     *
     * @return 現在のClipFrame
     */
    public synchronized long getClipFrame() {
        return this.clipFrame;
    }

    /**
     *
     * @return 書き込むファイルの拡張子
     */
    public synchronized FileExtension getExtension() {
        return this.ext;
    }

    /* ========================= 以下 setter =========================*/

    /**
     *
     * @param cycleTime 開いているファイルのcycleTime
     */
    public synchronized void setCycleTime(double cycleTime) {
        if(cycleTime != 0) {
            this.cycleTime = cycleTime;
        }
    }

    /**
     *
     * @param file 開くファイル
     * @param loadedFunc ファイルを開けた際に実行する関数
     */
    public synchronized void setExternalFile(File file, @Nullable IFunction<Void> loadedFunc) {
        this.isLoaded = false;
        this.file = file;
        this.loadedFunc = loadedFunc;
    }

    /**
     *
     * @param replayInitialFrame 開いているファイルのinitialFrame
     */
    public synchronized void setReplayInitialFrame(long replayInitialFrame) {
        this.replayInitialFrame = replayInitialFrame;
    }

    /**
     *
     * @param replayLastFrame 開いているファイルのlastFrame
     */
    public synchronized void setReplayLastFrame(long replayLastFrame) {
        this.replayLastFrame = replayLastFrame;
    }

    /**
     *
     * @param clipState clipStateを設定する
     */
    public synchronized void setClipState(ClipState clipState) {
        this.clipState = clipState;
    }

    /**
     * 再生するexternalを設定し、externalのinitFrameとlastFrameとcycleTimeを設定する
     */
    public synchronized void setLoadedFile(ReaderExternal external) {
        this.replayExternal = Optional.of(external);
        if (this.loadedFunc != null) {
            this.loadedFunc.function();
        }
        this.isLoaded = true;
    }

    /**
     *
     * @param replayFrame 現在のreplayFrameを設定する
     */
    public synchronized void setReplayFrame(long replayFrame) {
        this.replayFrame = Math.clamp(replayFrame, this.replayInitialFrame, this.replayLastFrame);
    }

    /**
     *
     * @param replayState replayStateを設定する
     */
    public synchronized void setReplayState(ReplayState replayState) {
        this.replayState = replayState;
    }

    /**
     *
     * @param percent ファイル読み込みの進捗
     */
    public synchronized void setPercent(int percent) {
        this.percent = Math.clamp(percent, 0, 100);
    }

    /**
     * @implNote binにformatが無い場合、Gsonの仕様により0が代入される
     * @param format RecordDataのformat
     */
    public synchronized void setFormat(int format) {
        this.format = format;
    }

    /**
     *
     * @param clipInitialFrame 切り取るファイルのinitialFrame
     */
    public synchronized void setClipInitialFrame(long clipInitialFrame) {
        this.clipInitialFrame = Optional.of(clipInitialFrame);
    }

    /**
     *
     * @param clipLastFrame 切り取るファイルのlastFrame
     */
    public synchronized void setClipLastFrame(long clipLastFrame) {
        this.clipLastFrame = Optional.of(clipLastFrame);
    }

    /**
     *
     * @param clipFrame 切り取るファイルの現在のフレーム
     */
    public synchronized void setClipFrame(long clipFrame) {
        this.clipFrame = clipFrame;
    }

    /**
     *
     * @implNote 切り取るファイルで、何行飛ばしで切り取るか設定する
     */
    public synchronized void setWindowSize(long windowSize) {
        this.windowSize = windowSize;
    }

    /**
     *
     * @param ext 切り取ったファイルの拡張子を設定する
     */
    public synchronized void setFileExtension(FileExtension ext) {
        if(!this.clipState.equals(ClipState.CLIP)) {
            this.ext = ext;
        }
    }

    /**
     *
     * @implNote .(ドット)不要<p></p>
     *           切り取り中でなければ設定する
     * @param fileName 切り取ったファイルの名前を設定する
     */
    public synchronized void setClipFileName(String fileName) {
        if(!this.clipState.equals(ClipState.CLIP)) {
            this.clipFileName = Optional.of(fileName);
        }
    }

    /**
     * 切り取り機能をリセットする
     */
    public synchronized void clipClose() {
        if(this.clipWriterExternal != null) {
            this.clipWriterExternal.close();
            this.clipWriterExternal = null;
        }
        this.clipFileName = Optional.empty();
        this.ext = FileExtension.CSV;
        this.clipInitialFrame = Optional.empty();
        this.clipLastFrame = Optional.empty();
        this.clipState = ClipState.WAIT;
        this.windowSize = 10L;
    }

    /**
     * 録画機能をリセットする
     */
    public synchronized void replayClose() {
        if (this.replayExternal.isPresent()) {
            this.replayExternal.get().close();
            this.replayState = ReplayState.STOP;
            this.replayExternal = Optional.empty();
            this.format = -1;
            if(this.logger != null) {
                this.logger.info("{} is closed.", this.file);
            }
            this.file = null;
            this.isLoaded = false;
            System.gc();
        }
    }
}
