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
import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;
import vn.fpt.seima.seimaserver.entity.NotificationType;
import vn.fpt.seima.seimaserver.mapper.TransactionMapper;
import vn.fpt.seima.seimaserver.repository.*;
import vn.fpt.seima.seimaserver.service.BudgetService;
import vn.fpt.seima.seimaserver.service.NotificationService;
import vn.fpt.seima.seimaserver.service.RedisService;
import vn.fpt.seima.seimaserver.service.TransactionService;
import vn.fpt.seima.seimaserver.service.WalletService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
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
    private final BudgetPeriodRepository budgetPeriodRepository;
    private final RedisService redisService;
    private final NotificationService notificationService;

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
            if (request.getCategoryId() == null) {
                throw new IllegalArgumentException("Category must not be null");
            }
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must greater than zero");
            }
            Transaction transaction = transactionMapper.toEntity(request);
            transaction.setUser(user);
            transaction.setCategory(category);
            transaction.setTransactionType(type);
            if (request.getGroupId() != null) {
                Group group = groupRepository.findById(request.getGroupId())
                        .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + request.getGroupId()));

                if (!groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), group.getGroupId())) {
                    throw new IllegalArgumentException("You are not authorized to create this group category.");
                }
                transaction.setGroup(group);
            } else {
                if (request.getWalletId() == null) {
                    throw new IllegalArgumentException("WalletId must not be null");
                }

                Wallet wallet = walletRepository.findById(request.getWalletId())
                        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
                transaction.setWallet(wallet);
                if (type == TransactionType.EXPENSE) {
                    budgetService.reduceAmount(user.getUserId(), request.getCategoryId(), transaction.getAmount(), transaction.getTransactionDate(), "EXPENSE", request.getCurrencyCode());
                    walletService.reduceAmount(request.getWalletId(), transaction.getAmount(), "EXPENSE", request.getCurrencyCode());
                }

                if (type == TransactionType.INCOME) {
                    budgetService.reduceAmount(user.getUserId(), request.getCategoryId(), transaction.getAmount(), transaction.getTransactionDate(), "INCOME", request.getCurrencyCode());
                    walletService.reduceAmount(request.getWalletId(), transaction.getAmount(), "INCOME", request.getCurrencyCode());
                }
                YearMonth month = YearMonth.from(transaction.getTransactionDate());
                String cacheKey = buildOverviewKey(transaction.getUser().getUserId(), month);
                String financialHealthKey = "financial_health:" + user.getUserId();
                redisService.delete(cacheKey);
                redisService.delete(financialHealthKey);            }
            Transaction savedTransaction = transactionRepository.save(transaction);

            // Send notification to all group members except current user if transaction is group-related
            if (savedTransaction.getGroup() != null) {
                try {
                    sendTransactionCreateNotificationToGroup(savedTransaction, user);
                } catch (Exception e) {
                    log.error("Failed to send transaction create notification for transaction {}: {}", 
                            savedTransaction.getTransactionId(), e.getMessage(), e);
                    // Don't throw exception to avoid affecting the main transaction flow
                }
            }

            return transactionMapper.toResponse(savedTransaction);
        } catch (Exception e) {
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

            if (request.getCategoryId() == null) {
                throw new IllegalArgumentException("CategoryId must not be null");
            }

            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            transaction.setCategory(category);

            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must greater than zero");
            }
            if(request.getGroupId()!= null) {
                Group  group = groupRepository.findById(request.getGroupId())
                        .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + request.getGroupId()));

                if (!groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), group.getGroupId())) {
                    throw new IllegalArgumentException("You are not authorized to create this group category.");
                }
                transaction.setGroup(group);
            }
            else{
                if (request.getWalletId() == null) {
                    throw new IllegalArgumentException("WalletId must not be null");
                }
                Wallet wallet = walletRepository.findById(request.getWalletId())
                        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
                transaction.setWallet(wallet);
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
                YearMonth month = YearMonth.from(transaction.getTransactionDate());
                String cacheKey = buildOverviewKey(transaction.getUser().getUserId(), month);
                String financialHealthKey = "financial_health:" + user.getUserId();
                redisService.delete(cacheKey);
                redisService.delete(financialHealthKey);
            }
            transactionMapper.updateTransactionFromDto(request, transaction);
            Transaction updatedTransaction = transactionRepository.save(transaction);

            // Send notification to all group members except current user if transaction is group-related
            if (updatedTransaction.getGroup() != null) {
                try {
                    sendTransactionUpdateNotificationToGroup(updatedTransaction, user);
                } catch (Exception e) {
                    log.error("Failed to send transaction update notification for transaction {}: {}", 
                            updatedTransaction.getTransactionId(), e.getMessage(), e);
                    // Don't throw exception to avoid affecting the main transaction flow
                }
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

        if (transaction.getGroup() != null) {
            Group group = groupRepository.findById(transaction.getGroup().getGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + transaction.getGroup().getGroupId()));

            if (!groupMemberRepository.existsByUserUserIdAndGroupGroupId(transaction.getUser().getUserId(), group.getGroupId())) {
                throw new IllegalArgumentException("You are not authorized to create this group category.");
            }
            transaction.setGroup(group);
        } else {

            YearMonth month = YearMonth.from(transaction.getTransactionDate());

            String cacheKey = buildOverviewKey(transaction.getUser().getUserId(), month);
            String financialHealthKey = "financial_health:" + transaction.getUser().getUserId();
            redisService.delete(cacheKey);
            redisService.delete(financialHealthKey);
            Wallet wallet = transaction.getWallet();
            if (transaction.getTransactionType() == TransactionType.EXPENSE) {
                wallet.setCurrentBalance(wallet.getCurrentBalance().add(transaction.getAmount()));
                List<BudgetCategoryLimit> budgetCategoryLimits = budgetCategoryLimitRepository.findByTransaction(transaction.getCategory().getCategoryId());

                if (!budgetCategoryLimits.isEmpty()) {
                    for (BudgetCategoryLimit budgetCategoryLimit : budgetCategoryLimits) {
                        Budget budget = budgetRepository.findByUserIdBudget(transaction.getUser().getUserId(), budgetCategoryLimit.getBudget().getBudgetId());

                        if (budget == null) {
                            continue;
                        }

                        List<BudgetPeriod> budgetPeriods = budgetPeriodRepository.findByBudget_BudgetIdAndTime(budget.getBudgetId(), transaction.getTransactionDate());
                        for (BudgetPeriod budgetPeriod : budgetPeriods) {
                            log.info("Checking BudgetPeriod id={}, transactionDate={}, startDate={}, endDate={}",
                                    budgetPeriod.getBudgetPeriodId(),
                                    transaction.getTransactionDate(),
                                    budgetPeriod.getStartDate(),
                                    budgetPeriod.getEndDate());

                            if (budgetPeriod.getStartDate() != null && budgetPeriod.getEndDate() != null) {
                                if (transaction.getTransactionDate().isBefore(budgetPeriod.getEndDate())
                                        && transaction.getTransactionDate().isAfter(budgetPeriod.getStartDate())) {

                                    log.info("Transaction {} falls within BudgetPeriod {} -> Updating remainingAmount",
                                            transaction.getTransactionId(), budgetPeriod.getBudgetPeriodId());

                                    budgetPeriod.setRemainingAmount(
                                            budgetPeriod.getRemainingAmount().add(transaction.getAmount()));
                                }
                            } else {
                                log.warn("BudgetPeriod {} has null startDate or endDate (startDate={}, endDate={})",
                                        budgetPeriod.getBudgetPeriodId(),
                                        budgetPeriod.getStartDate(),
                                        budgetPeriod.getEndDate());
                            }

                            budgetPeriodRepository.save(budgetPeriod);
                        }
                    }
                }

            } else {
                wallet.setCurrentBalance(wallet.getCurrentBalance().subtract(transaction.getAmount()));
            }
            walletRepository.save(wallet);
        }

        // Send notification to all group members except current user if transaction is group-related
        if (transaction.getGroup() != null) {
            try {
                sendTransactionDeleteNotificationToGroup(transaction, transaction.getUser());
            } catch (Exception e) {
                log.error("Failed to send transaction delete notification for transaction {}: {}",
                        transaction.getTransactionId(), e.getMessage(), e);
                // Don't throw exception to avoid affecting the main transaction flow
            }
        }

        transaction.setTransactionType(TransactionType.INACTIVE);
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
        final String key = String.format("tx:overview:%d:%s", userId, month);

        TransactionOverviewResponse cached = redisService.getObject(key, TransactionOverviewResponse.class);
        if (cached != null) {
            return cached;
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

        TransactionOverviewResponse result = TransactionOverviewResponse.builder()
                .summary(summary)
                .byDate(byDate)
                .build();

        redisService.set(key, result);
        redisService.setTimeToLiveInMinutes(key, 60*12);

        return result;
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
        List<User> listUser = new ArrayList<>();
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        listUser.add(currentUser);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        if (groupId != null) {
            List<GroupMember> groupMembers = groupMemberRepository.findActiveGroupMembers(groupId);
            for (GroupMember groupMember : groupMembers) {
                listUser.add(groupMember.getUser());

            }
        }
        List<Transaction> transactions =
                transactionRepository.listReportByUserAndCategoryAndTransactionDateBetween(listUser,categoryId, startDateTime, endDateTime, groupId );

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
     *
     * @param type period
     * @param categoryId cate of transaction
     * @param dateFrom date from of report
     * @param dateTo date to of report
     * @param groupId if have
     * @return TransactionCategoryReportResponse
     */

    @Override
    public TransactionCategoryReportResponse getCategoryReport(PeriodType type, Integer categoryId, LocalDate dateFrom, LocalDate dateTo, Integer groupId) {
        List<User> listUser = new ArrayList<>();
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        listUser.add(currentUser);
        if (categoryId == null) {
            throw new IllegalArgumentException("CategoryId must not be null");
        }
        if (groupId != null) {
            List<GroupMember> groupMembers = groupMemberRepository.findActiveGroupMembers(groupId);
            for (GroupMember groupMember : groupMembers) {
                listUser.add(groupMember.getUser());

            }
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
                } else if (days > 7) {
                    groupBy = "week";
                } else {
                    groupBy = "day";
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid type: " + type);
        }
        List<Transaction> transactions = transactionRepository.findExpensesByUserAndDateRange(
                categoryId, listUser, dateFrom.atStartOfDay(), dateTo.plusDays(1).atStartOfDay(), groupId);

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
    public TransactionDetailReportResponse getCategoryReportDetail(Integer categoryId, LocalDate dateFrom, LocalDate dateTo, Integer groupId) {
        List<User> listUser = new ArrayList<>();
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        listUser.add(currentUser);
        if (categoryId == null) {
            throw new IllegalArgumentException("CategoryId must not be null");
        }
        LocalDate now = LocalDate.now();
        if (dateFrom == null || dateTo == null) {
            dateFrom = now.withDayOfMonth(1);
            dateTo = now.withDayOfMonth(now.lengthOfMonth());
        }
        if (groupId != null) {
            List<GroupMember> groupMembers = groupMemberRepository.findActiveGroupMembers(groupId);
            for (GroupMember groupMember : groupMembers) {
                listUser.add(groupMember.getUser());

            }
        }
        List<Transaction> transactions = transactionRepository.findExpensesByUserAndDateRange(
                categoryId, listUser, dateFrom.atStartOfDay(), dateTo.atTime(23, 59, 59), groupId);

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

            }
            group.getTransactionDetailList().add(new TransactionDetailReportResponse.TransactionDetail(tx.getTransactionId(), tx.getTransactionType(),tx.getAmount() ,tx.getCurrencyCode(), tx.getTransactionDate(), tx.getDescription()) );
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
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        Budget budget = budgetRepository.findById(budgetId).orElseThrow(()
                -> new IllegalArgumentException("Not found budget with id: " + budgetId));
        List<Integer> categoryIds = new ArrayList<>();
        List<BudgetCategoryLimit> budgetCategoryLimits = budgetCategoryLimitRepository.findByBudget(budgetId);
        if (budgetCategoryLimits.isEmpty()) {
            throw new IllegalArgumentException("Not found budget with id: " + budgetId);
        }
        for (BudgetCategoryLimit budgetCategoryLimit : budgetCategoryLimits) {
            List<Category> categories = categoryRepository.getCategoriesByCategoryId(budgetCategoryLimit.getCategory().getCategoryId());
            if (categories.isEmpty()) {
                throw new IllegalArgumentException("Not found category with id: " + budgetCategoryLimit.getCategory().getCategoryId());
            }
            categoryIds.addAll(categories.stream().map(Category::getCategoryId).collect(Collectors.toList()));
        }
        LocalDateTime endDate = budget.getEndDate();
        if (budget.getEndDate() == null) {
            endDate = LocalDateTime.of(LocalDate.now().getYear(), 12, 31, 23, 59, 59);
        }
        Page<Transaction> transactions = transactionRepository.getTransactionByBudget(
                currentUser.getUserId(),
                categoryIds,
                budget.getStartDate(),
                endDate,
                pageable
        );
        return transactions.map(transactionMapper::toResponse);
    }

    @Override
    public TransactionWalletResponse getTransactionWallet(Integer id, LocalDate dateFrom, LocalDate dateTo, String type) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("WalletId must not be null");
        }

        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        LocalDateTime startDateTime = dateFrom.atStartOfDay();
        LocalDateTime endDateTime = dateTo.atTime(23, 59, 59);
        List<Transaction> transactions = new ArrayList<>();

        if (type == null) {
             transactions = transactionRepository.listTransactionByWallet(id, currentUser.getUserId(), startDateTime, endDateTime);

        }
        else {
            transactions = transactionRepository.listTransactionByAllWallet(id, currentUser.getUserId());
        }

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Map<String, List<TransactionWalletResponse.ReportByWallet>> reportByWallet = new TreeMap<>(Comparator.reverseOrder());

        for (Transaction transaction : transactions) {
            if (transaction.getTransactionType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(transaction.getAmount());
            } else if (transaction.getTransactionType() == TransactionType.EXPENSE) {
                totalExpense = totalExpense.add(transaction.getAmount());
            }

            String dateKey = transaction.getTransactionDate().toLocalDate().toString();
            TransactionWalletResponse.ReportByWallet reportItem = TransactionWalletResponse.ReportByWallet.builder()
                    .categoryId(transaction.getCategory().getCategoryId())
                    .categoryName(transaction.getCategory().getCategoryName())
                    .categoryIconUrl(transaction.getCategory().getCategoryIconUrl())
                    .amount(transaction.getAmount())
                    .balance(wallet.getCurrentBalance())
                    .transactionDate(transaction.getTransactionDate())
                    .transactionId(transaction.getTransactionId())
                    .transactionType(transaction.getTransactionType())
                    .build();

            reportByWallet.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(reportItem);
        }

        TransactionWalletResponse.Summary summary = TransactionWalletResponse.Summary.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .currentBalance(wallet.getCurrentBalance())
                .build();

        return TransactionWalletResponse.builder()
                .summary(summary)
                .reportByWallet(reportByWallet)
                .build();
    }

    /**
     * @return TransactionTodayResponse
     */
    @Override
    public List<TransactionTodayResponse> getTransactionToday() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        List<Transaction> transactions = transactionRepository.listTransactionToday(TransactionType.INACTIVE,currentUser.getUserId(),startOfDay,endOfDay);

        return transactions.stream()
                .map(transaction -> transactionMapper.transactionToday(transaction))
                .collect(Collectors.toList());
    }

    /**
     * @param startDate of transaction
     * @param endDate of transaction
     * @return TransactionReportResponse
     */
    @Override
    public TransactionReportResponse getTransactionChart(LocalDate startDate, LocalDate endDate) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Transaction> transactions =
                transactionRepository.listTransactionsChart(currentUser, startDateTime, endDateTime);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            BigDecimal amount = t.getAmount();

            if (t.getTransactionType() == TransactionType.EXPENSE) {
                totalExpense = totalExpense.add(amount);
            } else if (t.getTransactionType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(amount);
            }
        }

        BigDecimal balance = totalIncome.subtract(totalExpense);

        TransactionReportResponse.Summary summary = TransactionReportResponse.Summary.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(balance)
                .build();

        return TransactionReportResponse.builder()
                .summary(summary)
                .transactionsByCategory(null)
                .build();
    }

    /**
     * Sends notification to all group members except the current user when a transaction is updated
     */
    private void sendTransactionUpdateNotificationToGroup(Transaction transaction, User currentUser) {
        try {
            Integer groupId = transaction.getGroup().getGroupId();
            String groupName = transaction.getGroup().getGroupName();
            String currentUserName = currentUser.getUserFullName();
            String transactionDescription = transaction.getDescription() != null ? transaction.getDescription() : "No description";
            
            // Create notification message
            String title = "Transaction Updated";
            String message = String.format("%s updated a transaction in %s", currentUserName, groupName);
            String linkToEntity = String.format("seimaapp://groups/%d/transactions/%d", groupId, transaction.getTransactionId());
            
            // Send notification to all group members except current user
            notificationService.sendNotificationToGroupMembersExceptUser(
                groupId, 
                currentUser.getUserId(), 
                currentUserName, 
                NotificationType.TRANSACTION_UPDATED, 
                title, 
                message, 
                linkToEntity
            );
            
            log.info("Sent transaction update notification to group {} members for transaction {}", 
                    groupId, transaction.getTransactionId());
                    
        } catch (Exception e) {
            log.error("Failed to send transaction update notification for transaction {}: {}", 
                    transaction.getTransactionId(), e.getMessage(), e);
            // Don't throw exception to avoid affecting the main transaction flow
        }
    }

    /**
     * Sends notification to all group members except the current user when a transaction is created
     */
    private void sendTransactionCreateNotificationToGroup(Transaction transaction, User currentUser) {
        try {
            Integer groupId = transaction.getGroup().getGroupId();
            String groupName = transaction.getGroup().getGroupName();
            String currentUserName = currentUser.getUserFullName();
            String transactionDescription = transaction.getDescription() != null ? transaction.getDescription() : "No description";
            
            // Create notification message
            String title = "New Transaction";
            String transactionType = transaction.getTransactionType() == TransactionType.EXPENSE ? "expense" : "income";
            String currencyCode = transaction.getCurrencyCode() != null ? transaction.getCurrencyCode() : "VND";
            String message = String.format("%s added a new %s transaction: %s %s in %s", currentUserName, transactionType, transaction.getAmount(), currencyCode, groupName);
            String linkToEntity = String.format("seimaapp://groups/%d/transactions/%d", groupId, transaction.getTransactionId());
            
            // Send notification to all group members except current user
            notificationService.sendNotificationToGroupMembersExceptUser(
                groupId, 
                currentUser.getUserId(), 
                currentUserName, 
                NotificationType.TRANSACTION_CREATED, 
                title, 
                message, 
                linkToEntity
            );
            
            log.info("Sent transaction create notification to group {} members for transaction {}", 
                    groupId, transaction.getTransactionId());
                    
        } catch (Exception e) {
            log.error("Failed to send transaction create notification for transaction {}: {}", 
                    transaction.getTransactionId(), e.getMessage(), e);
            // Don't throw exception to avoid affecting the main transaction flow
        }
    }

    /**
     * Sends notification to all group members except the current user when a transaction is deleted
     */
    private void sendTransactionDeleteNotificationToGroup(Transaction transaction, User currentUser) {
        try {
            Integer groupId = transaction.getGroup().getGroupId();
            String groupName = transaction.getGroup().getGroupName();
            String currentUserName = currentUser.getUserFullName();
            String transactionDescription = transaction.getDescription() != null ? transaction.getDescription() : "No description";
            
            // Create notification message
            String title = "Transaction Removed";
            String message = String.format("%s removed a transaction in %s", currentUserName, groupName);
            String linkToEntity = String.format("seimaapp://groups/%d/transactions", groupId);
            
            // Send notification to all group members except current user
            notificationService.sendNotificationToGroupMembersExceptUser(
                groupId, 
                currentUser.getUserId(), 
                currentUserName, 
                NotificationType.TRANSACTION_DELETED, 
                title, 
                message, 
                linkToEntity
            );
            
            log.info("Sent transaction delete notification to group {} members for transaction {}", 
                    groupId, transaction.getTransactionId());
                    
        } catch (Exception e) {
            log.error("Failed to send transaction delete notification for transaction {}: {}", 
                    transaction.getTransactionId(), e.getMessage(), e);
            // Don't throw exception to avoid affecting the main transaction flow
        }
    }

    private String buildOverviewKey(Integer userId, YearMonth month) {
        return String.format("tx:overview:%d:%s", userId, month);
    }

}