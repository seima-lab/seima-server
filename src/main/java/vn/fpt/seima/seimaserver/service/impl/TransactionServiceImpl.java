package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.dto.request.transaction.CreateTransactionRequest;
import vn.fpt.seima.seimaserver.dto.response.transaction.*;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.mapper.TransactionMapper;
import vn.fpt.seima.seimaserver.repository.*;
import vn.fpt.seima.seimaserver.service.BudgetService;
import vn.fpt.seima.seimaserver.service.TransactionService;
import vn.fpt.seima.seimaserver.service.WalletService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final TransactionMapper transactionMapper;
    private final WalletService walletService;
    private final BudgetService budgetService;
    private final CacheManager cacheManager;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    private final BudgetRepository budgetRepository;

    @Override
    public Page<TransactionResponse> getAllTransaction( Pageable pageable) {
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        Page<Transaction> transactions = transactionRepository.findByType(TransactionType.INACTIVE,user.getUserId(),pageable);

        return transactions.map(transactionMapper::toResponse);
    }

    @Override
    public TransactionResponse getTransactionById(int id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + id));

        return transactionMapper.toResponse(transaction);
    }

    @Transactional
    public TransactionResponse saveTransaction(CreateTransactionRequest request, TransactionType type) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Request must not be null");
            }
            User user = UserUtils.getCurrentUser();
            if (user == null) {
                throw new IllegalArgumentException("User must not be null");
            }

            if (request.getWalletId() == null) {
                throw new IllegalArgumentException("WalletId must not be null");
            }
            if (request.getCategoryId() == null) {
                throw new IllegalArgumentException("Category must not be null");
            }

            Wallet wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            if(request.getAmount() == null || request.getAmount().equals(BigDecimal.ZERO)){
                throw new IllegalArgumentException("Amount must not be zero");
            }

            Transaction transaction = transactionMapper.toEntity(request);
            if(request.getGroupId()!= null) {
                Group  group = groupRepository.findById(request.getGroupId())
                        .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + request.getGroupId()));

                if (!groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), group.getGroupId())) {
                    throw new IllegalArgumentException("You are not authorized to create this group category.");
                }
                transaction.setGroup(group);
            }
            transaction.setUser(user);
            transaction.setCategory(category);
            transaction.setWallet(wallet);
            transaction.setTransactionType(type);

            if (type == TransactionType.EXPENSE) {
                budgetService.reduceAmount(user.getUserId(), request.getCategoryId(), transaction.getAmount(), transaction.getTransactionDate(), "EXPENSE", request.getCurrencyCode());
                walletService.reduceAmount(request.getWalletId(),transaction.getAmount(), "EXPENSE", request.getCurrencyCode());
            }

            if (type == TransactionType.INCOME) {
                budgetService.reduceAmount(user.getUserId(), request.getCategoryId(), transaction.getAmount(), transaction.getTransactionDate(), "INCOME", request.getCurrencyCode());
                walletService.reduceAmount(request.getWalletId(),transaction.getAmount(),"INCOME", request.getCurrencyCode());
            }

            Transaction savedTransaction = transactionRepository.save(transaction);

            YearMonth month = YearMonth.from(transaction.getTransactionDate());
            String cacheKey = transaction.getUser().getUserId() + "-" + month;
            Cache cache = cacheManager.getCache("transactionOverview");
            if (cache != null) {
                cache.evict(cacheKey);
            }
            return transactionMapper.toResponse(savedTransaction);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to create transaction: " + e.getMessage(), e);
        }

    }

    @Override
    @Transactional
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
                throw new IllegalArgumentException("CategoryId must not be null");
            }
            Wallet wallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
            transaction.setWallet(wallet);

            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            transaction.setCategory(category);

            if(request.getAmount() == null || request.getAmount().equals(BigDecimal.ZERO)){
                throw new IllegalArgumentException("Amount must not be zero");
            }
            if(request.getGroupId()!= null) {
                Group  group = groupRepository.findById(request.getGroupId())
                        .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + request.getGroupId()));

                if (!groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), group.getGroupId())) {
                    throw new IllegalArgumentException("You are not authorized to create this group category.");
                }
                transaction.setGroup(group);
            }
            BigDecimal newAmount = BigDecimal.ZERO;
            String type = null;

            //so sua lon hon cu
            if (transaction.getAmount().compareTo(request.getAmount()) < 0) {
                type = "update-subtract";
                newAmount = request.getAmount().subtract(transaction.getAmount());
                budgetService.reduceAmount(user.getUserId(), request.getCategoryId(), newAmount, transaction.getTransactionDate(),type , request.getCurrencyCode());
                walletService.reduceAmount(request.getWalletId(),newAmount, type, request.getCurrencyCode());

            } else if (transaction.getAmount().compareTo(request.getAmount()) > 0) {
                type = "update-add";
                newAmount = transaction.getAmount().subtract(request.getAmount());
                budgetService.reduceAmount(user.getUserId(), request.getCategoryId(), newAmount, transaction.getTransactionDate(),type, request.getCurrencyCode() );
                walletService.reduceAmount(request.getWalletId(),newAmount, type, request.getCurrencyCode());
            }
            else{
                type = "no-update";
                budgetService.reduceAmount(user.getUserId(), request.getCategoryId(),newAmount, transaction.getTransactionDate(),type, request.getCurrencyCode() );
                walletService.reduceAmount(request.getWalletId(),newAmount, type, request.getCurrencyCode());
            }
            transactionMapper.updateTransactionFromDto(request, transaction);

            Transaction updatedTransaction = transactionRepository.save(transaction);

            YearMonth month = YearMonth.from(transaction.getTransactionDate());
            String cacheKey = transaction.getUser().getUserId() + "-" + month;
            Cache cache = cacheManager.getCache("transactionOverview");
            if (cache != null) {
                cache.evict(cacheKey);
            }

            return transactionMapper.toResponse(updatedTransaction);

        } catch (Exception e) {
            throw new RuntimeException("Failed to update transaction: " + e.getMessage(), e);
        }
    }


    @Override
    @Transactional
    public void deleteTransaction(int id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + id));


        YearMonth month = YearMonth.from(transaction.getTransactionDate());
        String cacheKey = transaction.getUser().getUserId() + "-" + month;
        Cache cache = cacheManager.getCache("transactionOverview");
        if (cache != null) {
            cache.evict(cacheKey);
        }
        Wallet wallet = transaction.getWallet();
        if (transaction.getTransactionType() == TransactionType.EXPENSE) {

            wallet.setCurrentBalance(wallet.getCurrentBalance().add(transaction.getAmount()));

            List<BudgetCategoryLimit> budgetCategoryLimits = budgetCategoryLimitRepository.findByTransaction(transaction.getCategory().getCategoryId());

            if (budgetCategoryLimits.isEmpty()) {
                return;
            }
            for (BudgetCategoryLimit budgetCategoryLimit : budgetCategoryLimits) {
                Budget budget = budgetRepository.findById(budgetCategoryLimit.getBudget().getBudgetId())
                        .orElse(null);

                if (budget == null) {
                    continue;
                }
                if (transaction.getTransactionDate().isBefore(budget.getEndDate()) && transaction.getTransactionDate().isAfter(budget.getStartDate())){
                    budget.setBudgetRemainingAmount(budget.getBudgetRemainingAmount().add(transaction.getAmount()));
                    budgetRepository.save(budget);
                }
            }

        } else {
            wallet.setCurrentBalance(wallet.getCurrentBalance().subtract(transaction.getAmount()));
        }
        transaction.setTransactionType(TransactionType.INACTIVE);

        walletRepository.save(wallet);
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse recordExpense(CreateTransactionRequest request) {
        return saveTransaction(request, TransactionType.EXPENSE);
    }

    @Override
    @Transactional
    public TransactionResponse recordIncome(CreateTransactionRequest request) {
        return saveTransaction(request, TransactionType.INCOME);
    }

    @Override
    public TransactionResponse transferTransaction(CreateTransactionRequest request) {
        return saveTransaction(request, TransactionType.TRANSFER);
    }

    @Cacheable(value = "transactionOverview", key = "#userId + '-' + #month.toString()")
    public TransactionOverviewResponse getTransactionOverview(Integer userId, YearMonth month) {
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
                .findAllByUserAndTransactionDateBetween(userId,TransactionType.INACTIVE ,start, end);

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

    @Override
    public Page<TransactionResponse> viewHistoryTransactionsGroup(Pageable pageable, Integer groupId) {
        Page<Transaction> transactions = transactionRepository.findByTypeGroup(TransactionType.INACTIVE,groupId,pageable);

        return transactions.map(transactionMapper::toResponse);
    }

    @Override
    public Page<TransactionResponse> viewHistoryTransactionsDate(Pageable pageable, LocalDate startDate, LocalDate endDate, Integer groupId) {

        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Page<Transaction> transactions = transactionRepository.findByDate(TransactionType.INACTIVE,startDateTime, endDateTime, groupId,currentUser.getUserId(), pageable);

        return transactions.map(transactionMapper::toResponse);
    }

    @Override
    public TransactionReportResponse getTransactionReport(Integer categoryId,LocalDate startDate, LocalDate endDate, Integer groupId) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Transaction> transactions =
                transactionRepository.listReportByUserAndCategoryAndTransactionDateBetween(currentUser,categoryId, startDateTime, endDateTime, groupId );

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        Map<String, Map<Integer, TransactionReportResponse.ReportByCategory>> groupedByTypeAndCategory = new HashMap<>();
        groupedByTypeAndCategory.put("income", new HashMap<>());
        groupedByTypeAndCategory.put("expense", new HashMap<>());

        for (Transaction t : transactions) {
            String typeKey = t.getTransactionType().name().toLowerCase();
            BigDecimal amount = t.getAmount();
            Integer categoryIdKey = t.getCategory().getCategoryId();

            if (t.getTransactionType() == TransactionType.EXPENSE) {
                totalExpense = totalExpense.add(amount);
            } else if (t.getTransactionType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(amount);
            }

            Map<Integer, TransactionReportResponse.ReportByCategory> mapByCategory = groupedByTypeAndCategory.get(typeKey);
            TransactionReportResponse.ReportByCategory report = mapByCategory.get(categoryIdKey);

            if (report == null) {
                report = TransactionReportResponse.ReportByCategory.builder()
                        .categoryId(t.getCategory().getCategoryId())
                        .categoryName(t.getCategory().getCategoryName())
                        .categoryIconUrl(t.getCategory().getCategoryIconUrl())
                        .amount(amount)
                        .build();
                mapByCategory.put(categoryIdKey, report);
            } else {
                report.setAmount(report.getAmount().add(amount));
            }
        }
        Map<String, List<TransactionReportResponse.ReportByCategory>> transactionTypeMap = new HashMap<>();
        for (String type : groupedByTypeAndCategory.keySet()) {
            BigDecimal total = type.equals("expense") ? totalExpense : totalIncome;
            List<TransactionReportResponse.ReportByCategory> reportList = new ArrayList<>();

            for (TransactionReportResponse.ReportByCategory report : groupedByTypeAndCategory.get(type).values()) {
                double percent = total.compareTo(BigDecimal.ZERO) == 0 ? 0 :
                        report.getAmount().multiply(BigDecimal.valueOf(100))
                                .divide(total, 1, RoundingMode.HALF_UP)
                                .doubleValue();
                report.setPercentage(percent);
                reportList.add(report);
            }

            transactionTypeMap.put(type, reportList);
        }
        TransactionReportResponse.Summary summary = TransactionReportResponse.Summary.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome.subtract(totalExpense))
                .build();

        return TransactionReportResponse.builder()
                .summary(summary)
                .transactionsByCategory(transactionTypeMap)
                .build();
    }

    /**
     * @param categoryId
     * @param dateFrom
     * @param dateTo
     * @return TransactionCategoryReportResponse
     */


    @Override
    public TransactionCategoryReportResponse getCategoryReport(PeriodType type, Integer categoryId, LocalDate dateFrom, LocalDate dateTo) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("CategoryId must not be null");
        }

        LocalDate now = LocalDate.now();
        if (dateFrom == null || dateTo == null) {
            dateFrom = now.withDayOfMonth(1);
            dateTo = now.withDayOfMonth(now.lengthOfMonth());
        }

        long days = ChronoUnit.DAYS.between(dateFrom, dateTo) + 1;

        String groupBy;

        switch (type) {
            case PeriodType.DAILY:
            case PeriodType.WEEKLY:
                groupBy = "day";
                break;
            case PeriodType.MONTHLY:
                groupBy = "week";
                break;
            case PeriodType.YEARLY:
                groupBy = "month";
                dateFrom = LocalDate.of(now.getYear(), 1, 1);
                dateTo = now;;
                break;
            case PeriodType.CUSTOM:
                if (days > 30) {
                    groupBy = "month";
                } else {
                    groupBy = "week";
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid type: " + type);
        }
        List<Transaction> transactions = transactionRepository.findExpensesByUserAndDateRange(
                categoryId, currentUser.getUserId(), dateFrom.atStartOfDay(), dateTo.plusDays(1).atStartOfDay());

        Map<String, TransactionCategoryReportResponse.GroupAmount> result = new LinkedHashMap<>();
        Map<String, LocalDate> keyDateMap = new HashMap<>();


        for (Transaction tx : transactions) {
            LocalDate date = tx.getTransactionDate().toLocalDate();
            LocalDate sortDate;
            String key;
            switch (groupBy) {
                case "day":
                    key = date.toString();
                    sortDate = date;
                    break;

                case "week":
                    LocalDate firstDayOfMonth = dateFrom.withDayOfMonth(1);
                    LocalDate lastDayOfMonth = dateFrom.withDayOfMonth(dateFrom.lengthOfMonth());

                    LocalDate weekStart;
                    LocalDate weekEnd;

                    if (date.isBefore(firstDayOfMonth)) {
                        key = "before_month";
                        sortDate = date;
                        break;
                    }

                    if (!date.isAfter(firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)))) {
                        weekStart = firstDayOfMonth;
                        weekEnd = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                        if (weekEnd.isAfter(lastDayOfMonth)) weekEnd = lastDayOfMonth;
                    } else {
                        weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                        weekEnd = weekStart.plusDays(6);
                        if (weekEnd.isAfter(lastDayOfMonth)) weekEnd = lastDayOfMonth;
                    }

                    key = weekStart + "_to_" + weekEnd;
                    sortDate = weekStart;
                    break;

                case "month":
                    LocalDate monthStart = LocalDate.of(date.getYear(), date.getMonthValue(), 1);
                    LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                    LocalDate today = LocalDate.now();
                    if (monthEnd.isAfter(today)) {
                        monthEnd = today;
                    }
                    key = monthStart + "_to_" + monthEnd;
                    sortDate = monthStart;
                    break;

                default:
                    key = "unknown";
                    sortDate = date;
            }
            keyDateMap.put(key, sortDate);
            var item = result.getOrDefault(key, new
                    TransactionCategoryReportResponse.GroupAmount());
            if (tx.getTransactionType() == TransactionType.EXPENSE)
                item.setExpense(item.getExpense().add(tx.getAmount()));
            else if (tx.getTransactionType() == TransactionType.INCOME)
                item.setIncome(item.getIncome().add(tx.getAmount()));
            result.put(key, item);
        }

        int groupCount = result.size();
        BigDecimal totalExpense = result.values().stream().map(TransactionCategoryReportResponse.GroupAmount::getExpense).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIncome = result.values().stream().map(TransactionCategoryReportResponse.GroupAmount::getIncome).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgExpense = groupCount == 0 ? BigDecimal.ZERO : totalExpense.divide(BigDecimal.valueOf(groupCount), 2, RoundingMode.HALF_UP);
        BigDecimal avgIncome = groupCount == 0 ? BigDecimal.ZERO : totalIncome.divide(BigDecimal.valueOf(groupCount), 2, RoundingMode.HALF_UP);
        Map<String, TransactionCategoryReportResponse.GroupAmount> sortedResult = result.entrySet().stream()
                .sorted(Comparator.comparing(entry -> keyDateMap.get(entry.getKey())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        return new TransactionCategoryReportResponse(
                totalExpense, avgExpense, totalIncome, avgIncome, sortedResult, categoryId
        );
    }

    /**
     * @param categoryId
     * @param dateFrom
     * @param dateTo
     * @return
     */
    @Override
    public TransactionDetailReportResponse getCategoryReportDetail(Integer categoryId, LocalDate dateFrom, LocalDate dateTo) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("CategoryId must not be null");
        }
        LocalDate now = LocalDate.now();
        if (dateFrom == null || dateTo == null) {
            dateFrom = now.withDayOfMonth(1);
            dateTo = now.withDayOfMonth(now.lengthOfMonth());
        }

        List<Transaction> transactions = transactionRepository.findExpensesByUserAndDateRange(
                categoryId, currentUser.getUserId(), dateFrom.atStartOfDay(), dateTo.atTime(23, 59, 59));

        Map<String, TransactionDetailReportResponse.GroupDetail> result = new LinkedHashMap<>();
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalIncome = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            LocalDate txDate = tx.getTransactionDate().toLocalDate();
            String key = txDate.toString();

            TransactionDetailReportResponse.GroupDetail group = result.getOrDefault(key, new TransactionDetailReportResponse.GroupDetail());
            if (tx.getTransactionType() == TransactionType.EXPENSE) {
                group.setExpense(group.getExpense().add(tx.getAmount()));
                totalExpense = totalExpense.add(tx.getAmount());
            } else if (tx.getTransactionType() == TransactionType.INCOME) {
                group.setIncome(group.getIncome().add(tx.getAmount()));
                totalIncome = totalIncome.add(tx.getAmount());
            }

            if (group.getCategoryId() == null) {
                group.setCategoryId(tx.getCategory().getCategoryId());
                group.setCategoryName(tx.getCategory().getCategoryName());
                group.setCategoryIconUrl(tx.getCategory().getCategoryIconUrl());
                group.setTransactionId(tx.getTransactionId());
                group.setTransactionType(tx.getTransactionType());
                group.setTransactionDate(tx.getTransactionDate());
                group.setAmount(tx.getAmount());
                group.setCurrencyCode(tx.getCurrencyCode());
                group.setDescription(tx.getDescription());
            }

            result.put(key, group);
        }

        return new TransactionDetailReportResponse(totalExpense, totalIncome, result);
    }

    /**
     * @param budgetId budget of transactions
     * @param pageable paging date
     * @return transaction response
     */
    @Override
    public Page<TransactionResponse> getTransactionByBudget(Integer budgetId, Pageable pageable) {
        Page<Transaction> transactions =  null;
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        Budget budget = budgetRepository.findById(budgetId).orElseThrow(()
                -> new IllegalArgumentException("Not found budget with id: " + budgetId));

        List<BudgetCategoryLimit> budgetCategoryLimits = budgetCategoryLimitRepository.findByBudget(budgetId);
        if (budgetCategoryLimits.isEmpty()) {
            throw new IllegalArgumentException("Not found budget with id: " + budgetId);
        }
        for (BudgetCategoryLimit budgetCategoryLimit : budgetCategoryLimits) {
            List<Category> categories = categoryRepository.getCategoriesByCategoryId(budgetCategoryLimit.getCategory().getCategoryId());
            if (categories.isEmpty()) {
                throw new IllegalArgumentException("Not found category with id: " + budgetCategoryLimit.getCategory().getCategoryId());
            }
            for (Category category : categories) {
                transactions = transactionRepository.getTransactionByBudget(
                        currentUser.getUserId(),
                        category.getCategoryId(),
                        budget.getStartDate(),
                        budget.getEndDate(),
                        pageable
                );
            }
        }
        return transactions.map(transactionMapper::toResponse);
    }

}