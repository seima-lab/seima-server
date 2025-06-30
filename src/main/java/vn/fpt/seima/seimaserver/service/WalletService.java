package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.wallet.CreateWalletRequest;
import vn.fpt.seima.seimaserver.dto.response.wallet.WalletResponse;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    WalletResponse createWallet(CreateWalletRequest request);
    WalletResponse getWallet(Integer id);
    List<WalletResponse> getAllWallets();
    WalletResponse updateWallet(Integer id, CreateWalletRequest request);
    void deleteWallet(Integer id);
    void reduceAmount(Integer id, BigDecimal amount);
} 