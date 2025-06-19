package vn.fpt.seima.seimaserver.exception;

public class GroupException extends RuntimeException {
    public GroupException(String message) {
        super(message);
    }
    
    public GroupException(String message, Throwable cause) {
        super(message, cause);
    }
} 