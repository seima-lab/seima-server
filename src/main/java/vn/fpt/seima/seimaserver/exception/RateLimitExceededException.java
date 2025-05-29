package vn.fpt.seima.seimaserver.exception;

public class RateLimitExceededException  extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
