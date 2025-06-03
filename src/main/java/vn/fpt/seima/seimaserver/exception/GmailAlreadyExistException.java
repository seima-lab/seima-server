package vn.fpt.seima.seimaserver.exception;

public class GmailAlreadyExistException extends RuntimeException {
    public GmailAlreadyExistException(String message) {
        super(message);
    }
}
