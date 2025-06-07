package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.wallet.CreateWalletRequest;
import vn.fpt.seima.seimaserver.dto.response.wallet.WalletResponse;

import java.util.List;

public interface WalletService {
    WalletResponse createWallet(Integer userId, CreateWalletRequest request);
    WalletResponse getWallet(Integer userId, Integer id);
    List<WalletResponse> getAllWallets(Integer userId);
    WalletResponse updateWallet(Integer userId, Integer id, CreateWalletRequest request);
    void deleteWallet(Integer userId, Integer id);
} 