package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.wallet.request.CreateWalletRequest;
import vn.fpt.seima.seimaserver.dto.wallet.response.WalletResponse;
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
    public WalletResponse createWallet(CreateWalletRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));
        
        WalletType walletType = walletTypeRepository.findById(request.getWalletTypeId())
                .orElseThrow(() -> new RuntimeException("Wallet type not found with id: " + request.getWalletTypeId()));

        // Nếu đây là ví mặc định, cập nhật các ví khác
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            updateOtherWalletsDefaultStatus(user.getUserId(), false);
        }

        Wallet wallet = walletMapper.toEntity(request);
        wallet.setWalletCreatedAt(Instant.now());
        wallet.setUser(user);
        wallet.setWalletType(walletType);

        wallet = walletRepository.save(wallet);
        return walletMapper.toResponse(wallet);
    }

    @Override
    public WalletResponse getWallet(Integer id) {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wallet not found with id: " + id));
        return walletMapper.toResponse(wallet);
    }

    @Override
    public List<WalletResponse> getAllWallets() {
        return walletRepository.findAll().stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WalletResponse updateWallet(Integer id, CreateWalletRequest request) {
        Wallet existingWallet = walletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wallet not found with id: " + id));
        
        // Xử lý cập nhật ví mặc định trước khi update
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            updateOtherWalletsDefaultStatus(existingWallet.getUser().getUserId(), false);
        }

        // Update WalletType nếu có thay đổi
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
    public void deleteWallet(Integer id) {
        if (!walletRepository.existsById(id)) {
            throw new RuntimeException("Wallet not found with id: " + id);
        }
        walletRepository.deleteById(id);
    }

    private void updateOtherWalletsDefaultStatus(Integer userId, boolean isDefault) {
        walletRepository.findAll().stream()
            .filter(w -> w.getUser().getUserId().equals(userId))
            .forEach(w -> {
                w.setIsDefault(isDefault);
                walletRepository.save(w);
            });
    }
} 