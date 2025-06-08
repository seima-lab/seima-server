package vn.fpt.seima.seimaserver.exception;

public class NullRequestParamException extends RuntimeException {
    public NullRequestParamException(String message) {
        super(message);
    }
}
