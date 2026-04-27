package ai_server_cafe.util.reader;

import ai_server_cafe.util.interfaces.IFuncParam1;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReaderExternal {
    private final String path = "data";
    private final List<String> reader;
    private final File file;
    private final Logger logger;
    private final IFuncParam1<Void, Integer> percent;

    // IFunc1について
    // 1%上昇したときにprogressBar読み込み
    // updaterにファイル読み込み進捗管理のint変数とのやりとりを行う
    public ReaderExternal(File file, @Nullable Logger logger, Charset charset, IFuncParam1<Void, Integer> progressFunc, long size) {
        this.file = file;
        this.logger = logger;
        this.percent = progressFunc;
        this.reader = new ArrayList<>();
        if (size == -1) {
            size = 1000;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));){
            if(logger != null) logger.info("{} try to read.", file.getName());
            String str;
            int percent = 0;
            long count = 0;
            while ((str = reader.readLine()) != null) {
                this.reader.add(str);
                count++;
                if(percent < (int)((double)count * 100 / (double)size)) {
                    percent = (int)((double)count * 100 / (double)size);
                    progressFunc.function(percent);
                }
            }
            if(logger != null) logger.info("{} successfully read.", file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nonnull
    public Optional<String> readLine(long line) {
        if (this.reader.isEmpty() || line < 0 || this.reader.size() <= line) {
            return Optional.empty();
        }
        return Optional.of(this.reader.get((int)line));
    }

    @NonNull
    public Optional<String> lastLine() {
        if (this.reader.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(this.reader.getLast());
    }

    public void close() {
        if (this.reader != null) {
            if(this.logger != null) this.logger.info("Close file : {}", this.file.getName());
            this.percent.function(0);
        }
    }
}
