package wit.pap.multidraw.shared;

import wit.pap.multidraw.shared.globals.Globals;

public class LayeredImage {
    private BgraImage foreground, middleground, background;

    public LayeredImage() {
        byte p;

        background = new BgraImage();
        p = (byte) Globals.MAX_PIXEL;
        background.setAllBGRA(p, p, p, p);

        middleground = background.clone();

        foreground = new BgraImage();
        p = (byte) Globals.MIN_PIXEL;
        foreground.setAllBGRA(p, p, p, p);
    }

    // Getters & Setters -----------------------------------------------------------------------------------------------


    public BgraImage getForeground() {
        return foreground;
    }

    public void setForeground(BgraImage foreground) {
        this.foreground = foreground;
    }

    public BgraImage getMiddleground() {
        return middleground;
    }

    public void setMiddleground(BgraImage middleground) {
        this.middleground = middleground;
    }

    public BgraImage getBackground() {
        return background;
    }

    public void setBackground(BgraImage background) {
        this.background = background;
    }
}
