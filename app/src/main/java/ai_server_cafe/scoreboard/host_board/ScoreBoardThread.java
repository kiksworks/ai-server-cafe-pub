package ai_server_cafe.scoreboard.host_board;

import ai_server_cafe.config.Config;
import ai_server_cafe.scoreboard.host_board.protocol.MessageFromFrontend;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterScoreBoard;
import ai_server_cafe.util.thread.AbstractThreadCafe;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsCloseHandler;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsConnectHandler;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsErrorContext;
import io.javalin.websocket.WsErrorHandler;
import io.javalin.websocket.WsMessageContext;
import io.javalin.websocket.WsMessageHandler;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.function.Consumer;

public final class ScoreBoardThread extends AbstractThreadCafe {

    private static ScoreBoardThread instance = null;
    private final int port;

    private ScoreBoardThread() {
        super("scoreboard_thread");
        Config config = ConfigManager.getInstance().getConfig();
        this.port = config.scoreBoardPort;
        UpdaterScoreBoard.getInstance().addListener(new ScoreSender());
    }

    public static ScoreBoardThread getInstance() {
        if (instance == null) {
            instance = new ScoreBoardThread();
        }
        return instance;
    }

    Logger getLogger() {
        return logger;
    }

    @Override
    protected void runThread() {

        if (!isPortAvailable(port)) {
            logger.error("Port {} is already in use.", port);
            terminate();
            return;
        }

        Javalin app = Javalin.create(new Consumer<JavalinConfig>() {
            @Override
            public void accept(JavalinConfig javalinConfig) {
                javalinConfig.staticFiles.add("/scoreboard");
            }
        }).start(this.port);

        app.ws("/ws", new Consumer<WsConfig>() {
            @Override
            public void accept(WsConfig wsConfig) {
                wsConfig.onConnect(new WsConnectHandler() {
                    @Override
                    public void handleConnect(@Nonnull WsConnectContext wsConnectContext) throws Exception {
                        wsOnConnect(wsConnectContext);
                    }
                });
                wsConfig.onMessage(new WsMessageHandler() {
                    @Override
                    public void handleMessage(@Nonnull WsMessageContext wsMessageContext) throws Exception {
                        wsOnMessage(wsMessageContext);
                    }
                });
                wsConfig.onClose(new WsCloseHandler() {
                    @Override
                    public void handleClose(@Nonnull WsCloseContext wsCloseContext) throws Exception {
                        wsOnClose(wsCloseContext);
                    }
                });
                wsConfig.onError(new WsErrorHandler() {
                    @Override
                    public void handleError(@Nonnull WsErrorContext wsErrorContext) throws Exception {
                        logger.error("Error in websocket: {}", wsErrorContext.toString());
                    }
                });
            }
        });

        app.before("/ws", new Handler() {
            @Override
            public void handle(@Nonnull Context context) throws Exception {
                logger.info("connection established");
            }
        });

        app.get("/view", new Handler() {
            @Override
            public void handle(@Nonnull Context context) throws Exception {
                try (InputStream resourceStream = ScoreBoardThread.class.getResourceAsStream("/scoreboard/index.html")) {
                    if (resourceStream == null) {
                        context.status(404).result("404 Not Found");
                    } else {
                        context.contentType("text/html").result(new String(resourceStream.readAllBytes()));
                    }
                }
            }
        });

        HandleQR handleQR = new HandleQR();
        app.get("/qrcode", handleQR::handleQrCode);
        app.get("/url", handleQR::handleQRUrl);
        app.get("/qr", handleQR::handleQrPage);

        this.logger.info("Scoreboard server started on port {}", port);
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // WebSocket関連 //////////////////////////////////////////////////////////////////////////////

    // 接続時
    private void wsOnConnect(@Nonnull WsContext ctx) {
        for (UpdaterScoreBoard.ScoreUpdateListener listener : UpdaterScoreBoard.getInstance().getListeners().get()) {
            listener.addSession(ctx.session);
        }
        logger.info("Connection from: {}", ctx.session.getLocalAddress().toString());
    }

    // handle message
    private void wsOnMessage(@Nonnull WsMessageContext ctx) {
        String message = ctx.message();

        try {
            Gson gson = new Gson();
            MessageFromFrontend messageFromFrontend = gson.fromJson(message, MessageFromFrontend.class);
            String payload = messageFromFrontend.getPayload();

            switch (messageFromFrontend.getType()) {
                case "GameConfig": {
                    if ("reset".equals(payload)) {
                        UpdaterScoreBoard.getInstance().reset();
                        logger.debug("Game reset");
                    } else if ("start".equals(payload)) {
                        UpdaterScoreBoard.getInstance().startGame();
                        logger.debug("Game started");
                    } else {
                        logger.warn("(Game Config) Unknown payload: {}", payload);
                    }
                    break;
                }
                case "adjust-score": {
                    switch (payload) {
                        case "A+": {
                            UpdaterScoreBoard.getInstance().addScoreYellow(1);
                            break;
                        }
                        case "A-": {
                            UpdaterScoreBoard.getInstance().addScoreYellow(-1);
                            break;
                        }
                        case "B+": {
                            UpdaterScoreBoard.getInstance().addScoreBlue(1);
                            break;
                        }
                        case "B-": {
                            UpdaterScoreBoard.getInstance().addScoreBlue(-1);
                            break;
                        }
                        case null, default: {
                            logger.warn("(AdjustScore) unknown payload : {}", payload);
                            break;
                        }
                    }
                    logger.debug("current-score : {} - {}", UpdaterScoreBoard.getInstance().getScoreYellow(),
                            UpdaterScoreBoard.getInstance().getScoreBlue());
                    break;
                }
                case "adjust-time": {
                    try {
                        if (payload != null) {
                            UpdaterScoreBoard.getInstance().setGameDuration(Integer.parseInt(payload));
                            logger.debug("current-time : {}", UpdaterScoreBoard.getInstance().getGameDuration());
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("(AdjustTime) unknown payload : {}", payload);
                    }
                }
                case "heartbeat": {
                    logger.debug("Received Heartbeat from {}", ctx.session.getRemoteAddress());
                    ctx.session.setIdleTimeout(Duration.ofSeconds(60)); // 60秒に再設定
                    break;
                }
                default: {
                    logger.warn("Unknown message: {}", message);
                    break;
                }
            }
        } catch (JsonSyntaxException e) {
            logger.warn("Invalid message format: {}", message);
            ctx.send("Invalid message format");
        }

        if (UpdaterScoreBoard.getInstance().isDirty()) {
            for (UpdaterScoreBoard.ScoreUpdateListener listener : UpdaterScoreBoard.getInstance().getListeners().get()) {
                listener.onScoreUpdate();
            }

            UpdaterScoreBoard.getInstance().resetDirty();
        }
    }

    // 切断時
    private void wsOnClose(WsContext ctx){
        try{
            for (UpdaterScoreBoard.ScoreUpdateListener listener : UpdaterScoreBoard.getInstance().getListeners().get()) {
                listener.removeSession(ctx.session);
            }
            logger.warn("Connection closed from: {}", ctx.session.getLocalAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onTerminate() {
        UpdaterScoreBoard.getInstance().removeAllListener();
    }

    public static void reset() {
        instance = null;
    }
}
