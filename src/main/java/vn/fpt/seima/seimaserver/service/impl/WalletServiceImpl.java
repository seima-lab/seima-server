package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.wallet.CreateWalletRequest;
import vn.fpt.seima.seimaserver.dto.response.wallet.WalletResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.WalletException;
import vn.fpt.seima.seimaserver.mapper.WalletMapper;
import vn.fpt.seima.seimaserver.repository.TransactionRepository;
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
    private final TransactionRepository transactionRepository;

    @Override
    public WalletResponse createWallet(CreateWalletRequest request) {
        User currentUser = getCurrentUser();
        
        // Check wallet limit (maximum 5 wallets per user)
        List<Wallet> existingWallets = walletRepository.findAllActiveByUserId(currentUser.getUserId());
        if (existingWallets.size() >= 5) {
            throw new WalletException("Maximum wallet limit reached. You can only have up to 5 wallets.");
        }
        
        // Check wallet name uniqueness
        if (walletRepository.existsByUserIdAndWalletNameAndNotDeleted(currentUser.getUserId(), request.getWalletName())) {
            throw new WalletException("Wallet name already exists. Please choose a different name.");
        }
        
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
        
        // Set currency code if provided, otherwise use default
        if (request.getCurrencyCode() != null && !request.getCurrencyCode().trim().isEmpty()) {
            wallet.setCurrencyCode(request.getCurrencyCode());
        } else {
            wallet.setCurrencyCode("VND"); // Default currency
        }

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
        
        // Check wallet name uniqueness (exclude current wallet)
        if (walletRepository.existsByUserIdAndWalletNameAndNotDeletedAndIdNot(currentUser.getUserId(), request.getWalletName(), id)) {
            throw new WalletException("Wallet name already exists. Please choose a different name.");
        }
        
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            updateOtherWalletsDefaultStatus(currentUser.getUserId(), false);
        }

        if (request.getWalletTypeId() != null) {
            WalletType walletType = walletTypeRepository.findById(request.getWalletTypeId())
                    .orElseThrow(() -> new WalletException("Wallet type not found with id: " + request.getWalletTypeId()));
            existingWallet.setWalletType(walletType);
        }

        // Update currency code if provided
        BigDecimal income = transactionRepository.sumIncomeWallet(id, currentUser.getUserId());
        BigDecimal expense = transactionRepository.sumExpenseWallet(id, currentUser.getUserId());

        existingWallet.setCurrentBalance(existingWallet.getInitialBalance().add(income).subtract(expense));
        if (request.getCurrencyCode() != null && !request.getCurrencyCode().trim().isEmpty()) {
            existingWallet.setCurrencyCode(request.getCurrencyCode());
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
        
        // Check if this is the last remaining wallet
        List<Wallet> activeWallets = walletRepository.findAllActiveByUserId(currentUser.getUserId());
        if (activeWallets.size() <= 1) {
            throw new WalletException("Cannot delete the last remaining wallet. You must have at least one wallet.");
        }
        
        // Check if this is a default wallet being deleted
        boolean wasDefault = Boolean.TRUE.equals(wallet.getIsDefault());
        
        wallet.setIsDeleted(true);
        wallet.setDeletedAt(Instant.now());
        walletRepository.save(wallet);
        
        // If deleted wallet was default, automatically set another wallet as default
        if (wasDefault) {
            setAnotherWalletAsDefault(currentUser.getUserId(), id);
        }
    }

    private void updateOtherWalletsDefaultStatus(Integer userId, boolean isDefault) {
        walletRepository.findAllActiveByUserId(userId).stream()
            .forEach(w -> {
                w.setIsDefault(isDefault);
                walletRepository.save(w);
            });
    }

    private void setAnotherWalletAsDefault(Integer userId, Integer deletedWalletId) {
        List<Wallet> activeWallets = walletRepository.findAllActiveByUserId(userId);
        
        // Find the first wallet that is not the deleted one
        activeWallets.stream()
            .filter(w -> !w.getId().equals(deletedWalletId))
            .findFirst()
            .ifPresent(w -> {
                w.setIsDefault(true);
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
    public void reduceAmount(Integer id, BigDecimal amount, String type, String code) {
        Wallet existingWallet =  walletRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for this id: " + id));
        BigDecimal newAmount;
        if (existingWallet.getCurrencyCode().equals(code)) {
            if (type.equals("EXPENSE")){
                newAmount = existingWallet.getCurrentBalance().subtract(amount);
            }
            else if (type.equals("INCOME")){
                newAmount = existingWallet.getCurrentBalance().add(amount);
            }
            else if (type.equals("update-subtract")){
                newAmount = existingWallet.getCurrentBalance().subtract(amount);
            }
            else if (type.equals("update-add")) {
                newAmount = existingWallet.getCurrentBalance().add(amount);
            }
            else{
                newAmount = existingWallet.getCurrentBalance();
            }
            existingWallet.setCurrentBalance(newAmount);

            walletRepository.save(existingWallet);
        }
    }
} 
