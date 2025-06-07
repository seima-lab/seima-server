package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.wallet.CreateWalletRequest;
import vn.fpt.seima.seimaserver.dto.response.wallet.WalletResponse;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.entity.Wallet;
import vn.fpt.seima.seimaserver.entity.WalletType;
import vn.fpt.seima.seimaserver.mapper.WalletMapper;
import vn.fpt.seima.seimaserver.repository.UserRepository;
import vn.fpt.seima.seimaserver.repository.WalletRepository;
import vn.fpt.seima.seimaserver.repository.WalletTypeRepository;
import vn.fpt.seima.seimaserver.service.WalletService;

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
    public WalletResponse createWallet(Integer userId, CreateWalletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        WalletType walletType = walletTypeRepository.findById(request.getWalletTypeId())
                .orElseThrow(() -> new RuntimeException("Wallet type not found with id: " + request.getWalletTypeId()));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            updateOtherWalletsDefaultStatus(userId, false);
        }

        Wallet wallet = walletMapper.toEntity(request);
        wallet.setWalletCreatedAt(Instant.now());
        wallet.setUser(user);
        wallet.setWalletType(walletType);
        wallet.setIsDeleted(false);

        wallet = walletRepository.save(wallet);
        return walletMapper.toResponse(wallet);
    }

    @Override
    public WalletResponse getWallet(Integer userId, Integer id) {
        Wallet wallet = walletRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new RuntimeException("Wallet not found with id: " + id));
        validateUserOwnership(userId, wallet);
        return walletMapper.toResponse(wallet);
    }

    @Override
    public List<WalletResponse> getAllWallets(Integer userId) {
        return walletRepository.findAllActiveByUserId(userId).stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WalletResponse updateWallet(Integer userId, Integer id, CreateWalletRequest request) {
        Wallet existingWallet = walletRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new RuntimeException("Wallet not found with id: " + id));
        validateUserOwnership(userId, existingWallet);
        
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            updateOtherWalletsDefaultStatus(userId, false);
        }

        if (request.getWalletTypeId() != null) {
            WalletType walletType = walletTypeRepository.findById(request.getWalletTypeId())
                    .orElseThrow(() -> new RuntimeException("Wallet type not found with id: " + request.getWalletTypeId()));
            existingWallet.setWalletType(walletType);
        }

        walletMapper.updateEntity(existingWallet, request);
        existingWallet = walletRepository.save(existingWallet);
        return walletMapper.toResponse(existingWallet);
    }

    @Override
    public void deleteWallet(Integer userId, Integer id) {
        Wallet wallet = walletRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new RuntimeException("Wallet not found with id: " + id));
        validateUserOwnership(userId, wallet);
        
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
            throw new RuntimeException("User does not own this wallet");
        }
    }
} 