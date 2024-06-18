package wit.pap.multidraw.shared.globals;

public class Globals {
    public static final int IMAGE_WIDTH = 1920;
    public static final int IMAGE_HEIGHT = 1080;
    public static final int BGRA_CHANNELS = 4;
    public static final int IMAGE_ARR_LENGTH = IMAGE_WIDTH * IMAGE_HEIGHT * BGRA_CHANNELS;

    public static final int MIN_PIXEL = 0;
    public static final int MAX_PIXEL = 255;


    public static final int WINDOW_INITIAL_WIDTH = 1270;
    public static final int WINDOW_INITIAL_HEIGHT = 768;

    public static final int WINDOW_MIN_W = 960;
    public static final int WINDOW_MIN_H = 512;


    public static final int DEAD_USERS_CHECK_INTERVAL_SECONDS = 20;
    public static final int DEAD_ROOM_CHECK_INTERVAL_SECONDS = 20;
    public static final int MAX_ROOM_LINGER_MINUTES = 1;
    public static final int MIDDLEGROUND_CREATION_INTERVAL_SECONDS = 5;
}
