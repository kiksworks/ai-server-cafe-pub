package ai_server_cafe.util.natives;

import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class NativeExtractor {
    private final String targetDir;
    private final String targetFile;
    private final String destinationDir;

    /**
     * resourcesに格納されたnativeファイルなどを解凍する
     * @param targetDir "/"区切り
     * @param targetFile 拡張子を含む
     * @param destinationDir "/"区切り
     */
    public NativeExtractor(String targetDir, String targetFile, @Nonnull String destinationDir) {
        this.targetDir = targetDir;
        this.targetFile = targetFile;
        this.destinationDir = destinationDir.replace("/", File.separator);
    }

    public void load(Logger logger) {
        File dir = new File(this.destinationDir);
        if (!dir.exists() && dir.mkdirs()) {
            logger.info("Created new directory : {}", dir);
        }
        try {
            if (!new File(this.destinationDir + File.separator + this.targetFile).exists()) {
                OutputStream os = new FileOutputStream(this.destinationDir + File.separator + this.targetFile);
                InputStream is = Objects.requireNonNull(getClass().getResourceAsStream("/" + this.targetDir + "/" + this.targetFile));
                is.transferTo(os);
                os.close();
                is.close();
                logger.info("Successfully {} was extracted.", this.targetFile);
            }
        } catch (IOException e) {
            logger.warn("Skipping extraction {} with I/O exception.", this.targetFile);
        } catch (NullPointerException e) {
            logger.warn("Skipping loading {} with null pointer exception.", this.targetFile);
        }
    }
}
