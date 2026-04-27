package ai_server_cafe.scoreboard.host_board;

import ai_server_cafe.updater.ConfigManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.javalin.http.Context;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class HandleQR {

    Logger logger;
    private final String url;

    HandleQR() {
        this.logger = ScoreBoardThread.getInstance().getLogger();
        this.url =  "http://" + getLocalIpAddress() + ":" + ConfigManager.getInstance().getConfig().scoreBoardPort + "/view";;
    }

    void handleQrCode(@Nonnull Context context){

        try {
            BufferedImage qrImage = generateQRCode(url, 200, 200);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            context.contentType("image/png").result(baos.toByteArray());
        } catch (Exception e) {
            logger.error("Failed to generate QR code", e);
            context.status(500).result("Error generating QR code");
        }
    }

    void handleQRUrl(Context ctx){
        ctx.result(url);
    }

    void handleQrPage(@Nonnull Context context){
        try (InputStream resourceStream = getClass().getResourceAsStream("/scoreboard/qrPage.html")) {
            if (resourceStream == null) {
                context.status(404).result("404 Not Found");
            } else {
                context.contentType("text/html").result(new String(resourceStream.readAllBytes()));
            }
        } catch (IOException e) {
            logger.error("Failed to read qrPage.html");
            context.status(500).result("Error reading qrPage.html");
        }
    }


    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            logger.error("Failed to get local IP address", e);
        }
        return "127.0.0.1"; // 取得できなかった場合
    }

    private BufferedImage generateQRCode(String text, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        return image;
    }


}
