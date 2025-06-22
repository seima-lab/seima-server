package vn.fpt.seima.seimaserver.exception;

public class UserAccountNotActiveException extends RuntimeException {
    public UserAccountNotActiveException(String message) {
        super(message);
    }
} 