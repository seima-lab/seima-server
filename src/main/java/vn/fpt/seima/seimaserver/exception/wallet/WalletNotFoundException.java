package vn.fpt.seima.seimaserver.exception.wallet;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(String message) {
        super(message);
    }
} 