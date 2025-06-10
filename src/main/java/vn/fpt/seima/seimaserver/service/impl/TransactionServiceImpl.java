package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.transaction.CreateTransactionRequest;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOverviewResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.TransactionMapper;
import vn.fpt.seima.seimaserver.repository.BudgetRepository;
import vn.fpt.seima.seimaserver.repository.CategoryRepository;
import vn.fpt.seima.seimaserver.repository.TransactionRepository;
import vn.fpt.seima.seimaserver.repository.WalletRepository;
import vn.fpt.seima.seimaserver.service.CloudinaryService;
import vn.fpt.seima.seimaserver.service.TransactionService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final TransactionMapper transactionMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    public Page<TransactionResponse> getAllTransaction(Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findAll(pageable);

        return transactions.map(transactionMapper::toResponse);
    }

    @Override
    public TransactionResponse getTransactionById(int id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + id));

        return transactionMapper.toResponse(transaction);
    }

    public TransactionResponse saveTransaction(CreateTransactionRequest request, TransactionType type) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Request must not be null");
            }
            System.out.println("haha" + request.getWalletId());
            System.out.println("haha" + request.getTransactionType());
            User user = UserUtils.getCurrentUser();
            if (user == null) {
                throw new IllegalArgumentException("User must not be null");
            }

            if (request.getWalletId() == null) {
                throw new IllegalArgumentException("WalletId must not be null");
            }
            if (request.getCategoryId() == null) {
                throw new IllegalArgumentException("WalletId must not be null");
            }

            Wallet wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            if(request.getAmount().equals(BigDecimal.ZERO)){
                throw new IllegalArgumentException("Amount must not be zero");
            }

            Transaction transaction = transactionMapper.toEntity(request);
            if (request.getReceiptImageUrl() != null && !request.getReceiptImageUrl().isEmpty()) {
                Map uploadResult = cloudinaryService.uploadImage(
                        request.getReceiptImageUrl(), "transaction/receipt"
                );
                transaction.setReceiptImageUrl((String) uploadResult.get("secure_url"));
            }

            transaction.setUser(user);
            transaction.setCategory(category);
            transaction.setWallet(wallet);
            transaction.setTransactionType(type);
            Transaction savedTransaction = transactionRepository.save(transaction);

            return transactionMapper.toResponse(savedTransaction);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to create transaction: " + e.getMessage(), e);
        }

    }

    @Override
    @CacheEvict(value = "overview", key = "#request.transactionDate.toLocalDate().withDayOfMonth(1).toString()")
    public TransactionResponse updateTransaction(Integer id, CreateTransactionRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Request must not be null");
            }

            Transaction transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

            User user = UserUtils.getCurrentUser();
            if (user == null) {
                throw new IllegalArgumentException("User must not be null");
            }
            if (request.getWalletId() == null) {
                throw new IllegalArgumentException("WalletId must not be null");
            }
            if (request.getCategoryId() == null) {
                throw new IllegalArgumentException("WalletId must not be null");
            }
            Wallet wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
            transaction.setWallet(wallet);

            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            transaction.setCategory(category);

            if(request.getAmount().equals(BigDecimal.ZERO)){
                throw new IllegalArgumentException("Amount must not be zero");
            }

            if (request.getReceiptImageUrl() != null && !request.getReceiptImageUrl().isEmpty()) {
                Map uploadResult = cloudinaryService.uploadImage(
                        request.getReceiptImageUrl(), "transaction/receipt"
                );
                transaction.setReceiptImageUrl((String) uploadResult.get("secure_url"));
            }

            transactionMapper.updateTransactionFromDto(request, transaction);

            Transaction updatedTransaction = transactionRepository.save(transaction);

            return transactionMapper.toResponse(updatedTransaction);

        } catch (Exception e) {
            throw new RuntimeException("Failed to update transaction: " + e.getMessage(), e);
        }
    }


    @Override
    @CacheEvict(value = "overview", key = "#request.transactionDate.toLocalDate().withDayOfMonth(1).toString()")
    public void deleteTransaction(int id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + id));

        transaction.setTransactionType(TransactionType.INACTIVE);
        transactionRepository.save(transaction);
    }

    @Override
    @CacheEvict(value = "overview", key = "#request.transactionDate.toLocalDate().withDayOfMonth(1).toString()")
    public TransactionResponse recordExpense(CreateTransactionRequest request) {
        return saveTransaction(request, TransactionType.EXPENSE);
    }

    @Override
    @CacheEvict(value = "overview", key = "#request.transactionDate.toLocalDate().withDayOfMonth(1).toString()")
    public TransactionResponse recordIncome(CreateTransactionRequest request) {
        return saveTransaction(request, TransactionType.INCOME);
    }

    @Override
    public TransactionResponse transferTransaction(CreateTransactionRequest request) {
        return saveTransaction(request, TransactionType.TRANSFER);
    }

    @Cacheable(value = "transactionOverview", key = "#userId + '-' + #month.toString()")
    public TransactionOverviewResponse getTransactionOverview(YearMonth month) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (month == null) {
            month = YearMonth.now();
        }
        if ((month.getMonthValue() < 0 || month.getMonthValue() > 12)) {
            throw new IllegalArgumentException("Month is not in range [0, 12]");
        }
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository
                .findAllByUserAndTransactionDateBetween(currentUser, start, end);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        Map<LocalDate, List<TransactionOverviewResponse.TransactionItem>> grouped =
                new TreeMap<>(Comparator.reverseOrder());

        for (Transaction transaction : transactions) {
            if (transaction.getTransactionType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(transaction.getAmount());
            } else if (transaction.getTransactionType() == TransactionType.EXPENSE) {
                totalExpense = totalExpense.add(transaction.getAmount());
            }

            LocalDate date = transaction.getTransactionDate().toLocalDate();
            grouped.computeIfAbsent(date, k -> new ArrayList<>())
                    .add(transactionMapper.toTransactionItem(transaction));
        }

        List<TransactionOverviewResponse.DailyTransactions> byDate = grouped.entrySet().stream()
                .map(entry -> new TransactionOverviewResponse.DailyTransactions(
                        entry.getKey(), entry.getValue()
                ))
                .collect(Collectors.toList());

        TransactionOverviewResponse.Summary summary = TransactionOverviewResponse.Summary.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome.subtract(totalExpense))
                .build();

       return TransactionOverviewResponse.builder()
                .summary(summary)
                .byDate(byDate)
                .build();
    }
}