package wit.pap.multidraw.server;

public class DuplicateNicknameException extends Exception {
    public DuplicateNicknameException(String message) {
        super(message);
    }

    public DuplicateNicknameException(String message, Throwable cause) {
        super(message, cause);
    }
}
