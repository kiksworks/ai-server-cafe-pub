package ai_server_cafe.gui;

import ai_server_cafe.gui.component.PopupMenuCafe;
import ai_server_cafe.gui.interfaces.AbstractPanelCafe;
import ai_server_cafe.model.field.Field;
import ai_server_cafe.records.ver0.RecordData0;
import ai_server_cafe.updater.ConfigManager;
import ai_server_cafe.updater.UpdaterReplay;
import ai_server_cafe.updater.UpdaterWorld;
import ai_server_cafe.util.gui.ColorHelper;
import ai_server_cafe.util.math.MathHelper;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.Optional;

public class VisionArea extends AbstractPanelCafe {
    private boolean isRotated;
    private final PopupMenuCafe<EnumGamePopupMenu> jp;
    private Optional<Vector2D> lastPressedPos;

    public VisionArea(int width, int height) {
        super(0, 0, width, height);
        this.isRotated = false;
        this.jp = new PopupMenuCafe<>(EnumGamePopupMenu.values());
        this.add(jp);
        this.lastPressedPos = Optional.empty();
    }

    @Override
    public void initPaint(Graphics2D graphics2D) {
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics2D.setColor(ColorHelper.GROUND_BLACK);
        graphics2D.fillRect(0, 0, this.getWidth(), this.getHeight());
        Field field = UpdaterWorld.getInstance().getField();
        double drawMaxWidth = field.getCarpetWidth();
        double drawMaxHeight = field.getCarpetHeight();
        if(UpdaterReplay.getInstance().getFormat() == 0) {
            // ver.0専用の機能
            Optional<RecordData0> data = UpdaterReplay.getInstance().getReplayFrameData(RecordData0.class).get();
            if (data.isPresent()) {
                drawMaxWidth = field.getCarpetWidth();
                drawMaxHeight = field.getCarpetHeight();
            }
        }
        this.isRotated = this.getWidth() < this.getHeight();
        AffineTransform affineTransform = graphics2D.getTransform();
        double rate = FastMath.min((this.isRotated ? (double)this.getWidth() : (double)this.getHeight()) / drawMaxHeight,
                (this.isRotated ? (double)this.getHeight() : (double)this.getWidth()) / drawMaxWidth);
        affineTransform.translate(this.getWidth() / 2.0 , this.getHeight() / 2.0);
        affineTransform.rotate(this.isRotated ? 0.5 * FastMath.PI : 0.0, 0.0, 0.0);
        affineTransform.scale(rate, -rate);
        graphics2D.setTransform(affineTransform);
    }

    @Override
    public boolean isVisibleConfig() {
        return ConfigManager.getInstance().getConfig().isVisionAreaVisible;
    }

    @Override
    public void onResizePanel(int width, int height) {
    }

    @Override
    public void onResize(int newX, int newY) {
    }

    public void mouseClicked(@Nonnull MouseEvent e) {
        if (e.getX() <= getWidth() && e.getY() <= getHeight()) {
            if (e.isControlDown())
                return;
            if (e.getButton() == 1 && e.getClickCount() == 1) {
                //左クリックの時の処理を書く
            }  else if(e.getButton() == 1 && e.getClickCount() >= 2 && e.getClickCount() % 2 == 0){
                //ダブル左クリックの時の処理を書く
                Vector2D result = this.getInvertTransform(new Vector2D(e.getX(), e.getY()));
                EnumGamePopupMenu.LOCATE_BALL.getFunc().function(result);
            } else if (e.getButton() == 3) {
                //右クリックの時の処理を書く
                this.jp.show(this, e.getX(), e.getY());
                Vector2D result = this.getInvertTransform(new Vector2D(e.getX(), e.getY()));
                this.jp.setActionPos((int)result.getX(), (int)result.getY());
            }
        }
    }

    public void mousePressed(@Nonnull MouseEvent e) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
        if (e.getX() <= getWidth() && e.getY() <= getHeight()) {
            if (e.getButton() == 1 && e.isControlDown()) {
                Vector2D result = this.getInvertTransform(new Vector2D(e.getX(), e.getY()));
                this.lastPressedPos = Optional.of(result);
            } else {
                this.lastPressedPos = Optional.empty();
            }
        }
    }

    public void mouseReleased(@Nonnull MouseEvent e) {
        if (e.getX() <= getWidth() && e.getY() <= getHeight()) {
            if (e.getButton() == 1) {
                if (e.isControlDown()) {
                    Vector2D result = this.getInvertTransform(new Vector2D(e.getX(), e.getY()));
                    if (this.lastPressedPos.isPresent()) {
                        EnumGamePopupMenu.setBallVel(this.lastPressedPos.get(),
                                result.subtract(this.lastPressedPos.get()).scalarMultiply(e.isShiftDown() ? 2.0 : 1.0));
                    }
                } else {
                    this.lastPressedPos = Optional.empty();
                }
            }
        }
    }

    @Nonnull
    private Vector2D getInvertTransform(@Nonnull Vector2D target) {
        Field field = UpdaterWorld.getInstance().getField();
        double drawMaxWidth = field.getCarpetWidth();
        double drawMaxHeight = field.getCarpetHeight();
        boolean isRotated = this.getWidth() < this.getHeight();
        double rate = FastMath.min((this.isRotated ? (double)this.getWidth() : (double)this.getHeight()) / drawMaxHeight,
                (this.isRotated ? (double)this.getHeight() : (double)this.getWidth()) / drawMaxWidth);
        boolean invert = ConfigManager.getInstance().getConfig().visibility.invertVisible;
        Vector2D relativePos = target.subtract(new Vector2D(0.5 * this.getWidth(), 0.5 * this.getHeight()));
        Vector2D rotated = MathHelper.applyRotation2D(relativePos, (isRotated ? -0.5 * FastMath.PI : 0.0) + (invert ? FastMath.PI : 0.0));
        return new Vector2D(rotated.getX(), -rotated.getY()).scalarMultiply(1.0 / rate);
    }
}
