package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.wallet.request.CreateWalletRequest;
import vn.fpt.seima.seimaserver.dto.wallet.response.WalletResponse;

import java.util.List;

public interface WalletService {
    WalletResponse createWallet(CreateWalletRequest request);
    WalletResponse getWallet(Integer id);
    List<WalletResponse> getAllWallets();
    WalletResponse updateWallet(Integer id, CreateWalletRequest request);
    void deleteWallet(Integer id);
} 