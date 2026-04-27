package ai_server_cafe.updater;

import ai_server_cafe.util.interfaces.WrapperWeakCloneable;
import ai_server_cafe.util.writer.WriterExternal;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class UpdaterRecord {
    private static UpdaterRecord instance = null;
    private WriterExternal writerExternal = null;

    private UpdaterRecord() {
    }

    public synchronized static UpdaterRecord getInstance() {
        if (instance == null) {
            instance = new UpdaterRecord();
        }
        return instance;
    }

    /**
     *
     * @return 書き込む録画ファイル
     */
    @Nonnull
    public synchronized WrapperWeakCloneable<WriterExternal> getRecordWriter() {
        if (this.writerExternal == null) {
            this.writerExternal = new WriterExternal(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss").format(LocalDateTime.now())
                            + "-v" +  ConfigManager.getInstance().getConfig().saveFormat + ".bin",
                    LogManager.getLogger("record-writer"), StandardCharsets.ISO_8859_1);
        }
        return new WrapperWeakCloneable<>(this.writerExternal);
    }

    public static void reset() {
        if (instance != null) {
            instance.getRecordWriter().get().close();
        }
        instance = null;
    }
}
