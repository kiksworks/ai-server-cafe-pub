package ai_server_cafe.updater;

import ai_server_cafe.util.gui.PatternLayoutCafe;
import ai_server_cafe.util.interfaces.WrapperWeakCloneable;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public final class UpdaterLogEvent {
    private static UpdaterLogEvent instance = null;
    private final StringWriter stringWriter;
    private UpdaterLogEvent() {
        this.stringWriter = new StringWriter();
        this.addLogAppender(this.stringWriter);
    }

    private void addLogAppender(Writer writer) {
        LoggerContext lc = LoggerContext.getContext(false);
        Configuration configuration = lc.getConfiguration();
        PatternLayoutCafe layout = new PatternLayoutCafe();//PatternLayout.createDefaultLayout(configuration);
        Appender appender = WriterAppender.createAppender(layout, null, writer, "AppenderCafe", false, true);
        appender.start();
        configuration.addAppender(appender);
        this.updateLoggers(appender, configuration);
    }

    private void updateLoggers(final Appender appender, @Nonnull final Configuration config) {
        for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
            loggerConfig.addAppender(appender, null, null);
        }
        config.getRootLogger().addAppender(appender, null, null);
    }

    private void removeAppender(String name) {
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
            loggerConfig.removeAppender(name);
        }
        config.getRootLogger().removeAppender(name);
    }

    public synchronized static UpdaterLogEvent getInstance() {
        if (instance == null) {
            instance = new UpdaterLogEvent();
        }
        return instance;
    }

    synchronized public WrapperWeakCloneable<StringWriter> getOutput() {
        return new WrapperWeakCloneable<>(this.stringWriter);
    }

    synchronized public void terminate() {
        this.removeAppender("logger");
        try {
            this.stringWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void reset() {
        instance = null;
    }
}
