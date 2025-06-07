package vn.fpt.seima.seimaserver.exception.wallet;

public class WalletAccessDeniedException extends RuntimeException {
    public WalletAccessDeniedException(String message) {
        super(message);
    }
} 