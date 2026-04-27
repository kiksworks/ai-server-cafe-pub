package ai_server_cafe.util.writer;

import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;

public class WriterExternal {
    private final String path = "data";
    private BufferedWriter writer;
    private final Logger logger;
    private final String fileName;
    public WriterExternal(String externalFileName, @Nullable Logger logger, Charset charset) {
        try {
            File dir = new File(this.path);
            if (dir.mkdir()) {
                if (logger != null) logger.info("Create new directory {}", dir.getAbsolutePath());
            }
            this.writer = new BufferedWriter(new FileWriter(this.path + File.separator + externalFileName, charset));
        } catch (IOException e) {
            if (logger != null) logger.warn("Couldn't create new file {}", externalFileName);
        }
        this.logger = logger;
        this.fileName = externalFileName;
    }

    public void append(String text) {
        if (this.writer != null) {
            try {
                this.writer.append(text);
            } catch (IOException e) {
                if (this.logger != null) this.logger.warn("Couldn't write to {}", this.path + File.separator + this.fileName);
            }
        }
    }

    public void close() {
        if (this.writer != null) {
            try {
                if(logger != null) logger.info("Close file : {}", fileName);
                this.writer.close();
            } catch (IOException e) {
                // DO nothing
            }
        }
    }
}
