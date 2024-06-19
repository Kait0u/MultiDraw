package wit.pap.multidraw.client.gui.widgets;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import wit.pap.multidraw.client.gui.utilities.Alerts;
import wit.pap.multidraw.shared.globals.Globals;
import wit.pap.multidraw.shared.BgraImage;
import wit.pap.multidraw.shared.LayeredImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class LayeredImageStack extends StackPane {
    private LayeredImage layeredImage;
    private ImageView bgImageView, mgImageView;
    private Canvas fgCanvas;

    public LayeredImageStack(LayeredImage layeredImage) {
        setPrefSize(Globals.IMAGE_WIDTH, Globals.IMAGE_HEIGHT);

        this.layeredImage = layeredImage;

        this.bgImageView = new ImageView();
        this.mgImageView = new ImageView();

        // Create canvas for drawing
        fgCanvas = new Canvas();
        fgCanvas.widthProperty().bind(this.widthProperty());
        fgCanvas.heightProperty().bind(this.heightProperty());


        getChildren().addAll(bgImageView, mgImageView, fgCanvas);

        // Draw images when the size of the stack pane changes
        widthProperty().addListener((obs, oldVal, newVal) -> draw());
        heightProperty().addListener((obs, oldVal, newVal) -> draw());

        // Initial drawing
        draw(true);
    }

    private void draw() {
        draw(false);
    }

    private void draw(boolean clear) {
        double width = getWidth();
        double height = getHeight();

        Image bgImage = layeredImage.getBackground().toWritableImage();
        Image mgImage = layeredImage.getMiddleground().toWritableImage();

        GraphicsContext gc = fgCanvas.getGraphicsContext2D();

        // Clear canvas
        if (clear)
            gc.clearRect(0, 0, width, height);

        // Draw background image
        if (bgImage != null) {
            bgImageView.setImage(bgImage);
        }

        // Draw top image
        if (mgImage != null) {
            mgImageView.setImage(mgImage);
        }
    }

    public void saveAsPNG(File outFile) {
        if (fgCanvas == null || layeredImage == null)
            return;

        WritableImage writableImage = new WritableImage(Globals.IMAGE_WIDTH, Globals.IMAGE_HEIGHT);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        fgCanvas.snapshot(params, writableImage);

        BgraImage bg = getLayeredImage().getBackground(),
                mg = getLayeredImage().getMiddleground(),
                fg = BgraImage.fromFXImage(writableImage);
        BgraImage result = BgraImage.overlayAll(bg, mg, fg);

        if (result != null) {
            BufferedImage bi = SwingFXUtils.fromFXImage(result.toWritableImage(), null);

            try {
                ImageIO.write(bi, "png", outFile);
            } catch (IOException e) {
                Alerts.showErrorAlert(e.toString(), e.getMessage());
            }
        }


    }

    // Getter for the canvas to allow external manipulation if necessary
    public Canvas getFgCanvas() {
        return fgCanvas;
    }

    public ImageView getBgImageView() {
        return bgImageView;
    }

    public ImageView getMgImageView() {
        return mgImageView;
    }

    public LayeredImage getLayeredImage() {
        return layeredImage;
    }

    public void setLayeredImage(LayeredImage layeredImage) {
        this.layeredImage = layeredImage;
        draw();
    }

    public void setMgImage(BgraImage image) {
        layeredImage.setMiddleground(image);
        draw();
    }

    public void setBgImage(BgraImage image) {
        layeredImage.setBackground(image);
        draw();
    }
}

