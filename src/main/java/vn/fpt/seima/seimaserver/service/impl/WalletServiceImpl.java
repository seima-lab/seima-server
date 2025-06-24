package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.wallet.CreateWalletRequest;
import vn.fpt.seima.seimaserver.dto.response.wallet.WalletResponse;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.entity.Wallet;
import vn.fpt.seima.seimaserver.entity.WalletType;
import vn.fpt.seima.seimaserver.exception.WalletException;
import vn.fpt.seima.seimaserver.mapper.WalletMapper;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.repository.WalletRepository;
import vn.fpt.seima.seimaserver.repository.WalletTypeRepository;
import vn.fpt.seima.seimaserver.service.WalletService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletTypeRepository walletTypeRepository;
    private final WalletMapper walletMapper;

    @Override
    public WalletResponse createWallet(CreateWalletRequest request) {
        User currentUser = getCurrentUser();
        
        WalletType walletType = walletTypeRepository.findById(request.getWalletTypeId())
                .orElseThrow(() -> new WalletException("Wallet type not found with id: " + request.getWalletTypeId()));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            updateOtherWalletsDefaultStatus(currentUser.getUserId(), false);
        }

        Wallet wallet = walletMapper.toEntity(request);
        wallet.setWalletCreatedAt(Instant.now());
        wallet.setUser(currentUser);
        wallet.setWalletType(walletType);
        wallet.setIsDeleted(false);

        wallet = walletRepository.save(wallet);
        return walletMapper.toResponse(wallet);
    }

    @Override
    public WalletResponse getWallet(Integer id) {
        User currentUser = getCurrentUser();
        
        Wallet wallet = walletRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new WalletException("Wallet not found with id: " + id));
        validateUserOwnership(currentUser.getUserId(), wallet);
        return walletMapper.toResponse(wallet);
    }

    @Override
    public List<WalletResponse> getAllWallets() {
        User currentUser = getCurrentUser();
        
        return walletRepository.findAllActiveByUserId(currentUser.getUserId()).stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WalletResponse updateWallet(Integer id, CreateWalletRequest request) {
        User currentUser = getCurrentUser();
        
        Wallet existingWallet = walletRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new WalletException("Wallet not found with id: " + id));
        validateUserOwnership(currentUser.getUserId(), existingWallet);
        
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            updateOtherWalletsDefaultStatus(currentUser.getUserId(), false);
        }

        if (request.getWalletTypeId() != null) {
            WalletType walletType = walletTypeRepository.findById(request.getWalletTypeId())
                    .orElseThrow(() -> new WalletException("Wallet type not found with id: " + request.getWalletTypeId()));
            existingWallet.setWalletType(walletType);
        }

        walletMapper.updateEntity(existingWallet, request);
        existingWallet = walletRepository.save(existingWallet);
        return walletMapper.toResponse(existingWallet);
    }

    @Override
    public void deleteWallet(Integer id) {
        User currentUser = getCurrentUser();
        
        Wallet wallet = walletRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new WalletException("Wallet not found with id: " + id));
        validateUserOwnership(currentUser.getUserId(), wallet);
        
        wallet.setIsDeleted(true);
        wallet.setDeletedAt(Instant.now());
        walletRepository.save(wallet);
    }

    private void updateOtherWalletsDefaultStatus(Integer userId, boolean isDefault) {
        walletRepository.findAllActiveByUserId(userId).stream()
            .forEach(w -> {
                w.setIsDefault(isDefault);
                walletRepository.save(w);
            });
    }

    private void validateUserOwnership(Integer userId, Wallet wallet) {
        if (!wallet.getUser().getUserId().equals(userId)) {
            throw new WalletException("User does not own this wallet");
        }
    }

    private User getCurrentUser() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new WalletException("Unable to identify the current user");
        }
        return currentUser;
    }

    @Override
    public void reduceAmount(Integer id, BigDecimal amount) {
        Wallet existingWallet =  walletRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for this id: " + id));

        BigDecimal newAmount = existingWallet.getCurrentBalance().subtract(amount);
        existingWallet.setCurrentBalance(newAmount);

        walletRepository.save(existingWallet);
    }
} 