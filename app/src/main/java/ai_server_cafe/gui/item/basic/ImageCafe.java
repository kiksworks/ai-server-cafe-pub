package ai_server_cafe.gui.item.basic;

import ai_server_cafe.gui.interfaces.AbstractGraphicalComponent;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.io.InputStream;

public class ImageCafe extends AbstractGraphicalComponent  {
    protected final BufferedImage image;
    protected final int sizeX;
    protected final int sizeY;

    public ImageCafe(Color color, String filePath, int sizeX, int sizeY) {
        super(color);
        InputStream imgStream = getClass().getResourceAsStream(filePath);
        assert imgStream != null;
        BufferedImage myImg = null;
        try {
            myImg = ImageIO.read(imgStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (myImg == null) {
            throw new RuntimeException(new NullPointerException("image is null : " + filePath));
        }
        this.image = myImg;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }

    @Override
    public void paint2D(Graphics2D graphics) {
    }

    @Override
    public void paint2D(@Nonnull Graphics2D graphics, ImageObserver observer) {
        AffineTransform transform = new AffineTransform();
        transform.scale(1.0, -1.0);
        transform.translate(-0.5 * this.sizeX, -0.5 * this.sizeY);
        graphics.drawImage(this.image.getScaledInstance(this.sizeX, this.sizeY, Image.SCALE_DEFAULT), transform, observer);
    }

    @Override
    public void paint(Graphics2D graphics) {

    }
}
