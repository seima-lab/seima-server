package vn.fpt.seima.seimaserver.exception;

public class PhoneNumberAlreadyExistsException extends RuntimeException{
    public PhoneNumberAlreadyExistsException(String message) {
        super(message);
    }
}
