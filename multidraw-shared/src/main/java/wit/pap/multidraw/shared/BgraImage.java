package wit.pap.multidraw.shared;

import javafx.scene.image.*;
import wit.pap.multidraw.shared.globals.Globals;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.stream.IntStream;

public class BgraImage implements Serializable {
    private byte[] imageArr;
    int width, height;

    private BgraImage(int length, int w, int h) {
        if (length != w * h * Globals.BGRA_CHANNELS)
            throw new IllegalArgumentException(
                    "The length parameter needs to equal width * height * the number of RGBA channels!"
            );

        imageArr = new byte[length];
        Arrays.fill(imageArr, (byte) Globals.MIN_PIXEL);

        width = w;
        height = h;
    }

    public BgraImage() {
        this(Globals.IMAGE_ARR_LENGTH, Globals.IMAGE_WIDTH, Globals.IMAGE_HEIGHT);
    }

    public BgraImage(int w, int h) {
        this(w * h * Globals.BGRA_CHANNELS, w, h);
    }

    private BgraImage(byte[] arr, int w, int h) {
        if (arr.length < w * h * Globals.BGRA_CHANNELS)
            throw new IllegalArgumentException("Size mismatch.");

        imageArr = arr;
        width = w;
        height = h;
    }

    // Methods ---------------------------------------------------------------------------------------------------------

    @Override
    public BgraImage clone() {
        byte[] arr = Arrays.copyOf(imageArr, imageArr.length);
        return new BgraImage(arr, width, height);
    }

    public void setBGRA(int x, int y, byte b, byte g, byte r, byte a) {
        int idx = ((y * width + x)) * Globals.BGRA_CHANNELS;

        imageArr[idx]     = b;
        imageArr[idx + 1] = g;
        imageArr[idx + 2] = r;
        imageArr[idx + 3] = a;
    }

    public void setRGBA(int x, int y, byte r, byte g, byte b, byte a) {
        setBGRA(x, y, b, g, r, a);
    }


    public void setAllBGRA(byte b, byte g, byte r, byte a) {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                setBGRA(x, y, b, g, r, a);
            }
        }
    }

    public void setAllRGBA(byte r, byte g, byte b, byte a) {
        setAllBGRA(b, g, r, a);
    }

    public WritableImage toWritableImage() {
        if (imageArr.length < Globals.IMAGE_ARR_LENGTH)
            throw new IllegalArgumentException("Byte array is not large enough for the specified dimensions.");

        WritableImage image = new WritableImage(Globals.IMAGE_WIDTH, Globals.IMAGE_HEIGHT);
        PixelWriter pixelWriter = image.getPixelWriter();

        pixelWriter.setPixels(
                0, 0, Globals.IMAGE_WIDTH, Globals.IMAGE_HEIGHT,
                PixelFormat.getByteBgraInstance(), imageArr,
                0, Globals.IMAGE_WIDTH * Globals.BGRA_CHANNELS
        );

        return image;
    }

    public static BgraImage createTransparent(int imageWidth, int imageHeight) {
        BgraImage result = new BgraImage(imageWidth, imageHeight);
        result.setAllBGRA(
                (byte) Globals.MIN_PIXEL,
                (byte) Globals.MIN_PIXEL,
                (byte) Globals.MIN_PIXEL,
                (byte) Globals.MIN_PIXEL
        );
        return result;
    }

    public static BgraImage fromFXImage(Image img) {
        int width = (int) img.getWidth();
        int height = (int) img.getHeight();

        BgraImage result = new BgraImage(width, height);
        byte[] byteArray = result.getImageArr();

        PixelReader pixelReader = img.getPixelReader();
        for (int y = 0, index = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int argb = pixelReader.getArgb(x, y);
                System.out.println(Integer.toString(index)+ " " + Integer.toString(argb));
                byteArray[index++] = (byte) (argb & 0xFF);         // Blue
                byteArray[index++] = (byte) ((argb >> 8) & 0xFF);  // Green
                byteArray[index++] = (byte) ((argb >> 16) & 0xFF); // Red
                byteArray[index++] = (byte) ((argb >> 24) & 0xFF); // Alpha
            }
        }

        return result;
    }

    public static BgraImage overlay(BgraImage bottom, BgraImage top) {
        if (bottom == null || top == null)
            throw new IllegalArgumentException("Images cannot be null!");

        byte[] bottomBytes = bottom.getImageArr();
        byte[] topBytes = top.getImageArr();
        byte[] resultBytes = new byte[topBytes.length];

        IntStream.range(0, resultBytes.length).parallel()
                .filter(idx -> idx % Globals.BGRA_CHANNELS == 0)
                .forEach(idx -> {
                    int bBottom = bottomBytes[idx] & 0xFF;
                    int gBottom = bottomBytes[idx + 1] & 0xFF;
                    int rBottom = bottomBytes[idx + 2] & 0xFF;
                    int aBottom = bottomBytes[idx + 3] & 0xFF;

                    int bTop = topBytes[idx] & 0xFF;
                    int gTop = topBytes[idx + 1] & 0xFF;
                    int rTop = topBytes[idx + 2] & 0xFF;
                    int aTop = topBytes[idx + 3] & 0xFF;

                    float alpha = aTop / (float) Globals.MAX_PIXEL;

                    int bResult = (int) (bTop * alpha + bBottom * (1 - alpha));
                    int gResult = (int) (gTop * alpha + gBottom * (1 - alpha));
                    int rResult = (int) (rTop * alpha + rBottom * (1 - alpha));
                    int aResult = Integer.max(aTop, aBottom);

                    resultBytes[idx] = (byte) bResult;
                    resultBytes[idx + 1] = (byte) gResult;
                    resultBytes[idx + 2] = (byte) rResult;
                    resultBytes[idx + 3] = (byte) aResult;
                }
        );

        return new BgraImage(resultBytes, top.getWidth(), top.getHeight());
    }

    public BgraImage overlay(BgraImage other) {
        if (other == null)
            throw new InvalidParameterException("Image cannot be null!");

        return BgraImage.overlay(this, other);
    }

    public static BgraImage overlayAll(BgraImage... images) {
        if (images.length == 0) return null;
        else if (images.length == 1) return images[0];
        else {
            BgraImage result = images[0].clone();

            for (int idx = 1; idx < images.length; ++idx)
                result = result.overlay(images[idx]);

            return result;
        }
    }

    // Getters & Setters -----------------------------------------------------------------------------------------------

    public byte[] getImageArr() {
        return imageArr;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
