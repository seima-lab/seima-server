package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.request.transaction.CreateTransactionRequest;
import vn.fpt.seima.seimaserver.dto.response.transaction.*;
import vn.fpt.seima.seimaserver.entity.TransactionType;

import java.time.LocalDate;
import java.time.YearMonth;

public interface TransactionService {

    Page<TransactionResponse> getAllTransaction(Pageable pageable);

    TransactionResponse getTransactionById(int id);

//    TransactionResponse saveTransaction(CreateTransactionRequest request);

    TransactionResponse updateTransaction(Integer id,CreateTransactionRequest budget);

    void deleteTransaction(int id);

    TransactionResponse recordExpense(CreateTransactionRequest request);

    TransactionResponse recordIncome(CreateTransactionRequest request);

    TransactionResponse transferTransaction(CreateTransactionRequest request);

    TransactionOverviewResponse getTransactionOverview(Integer userId, YearMonth month);

    Page<TransactionResponse> viewHistoryTransactionsGroup(Pageable pageable, Integer groupId);

    Page<TransactionResponse> viewHistoryTransactionsDate(Pageable pageable, LocalDate startDate, LocalDate endDate);

    TransactionReportResponse getTransactionReport(Integer categoryId,LocalDate startDate, LocalDate endDate);

    TransactionCategoryReportResponse getCategoryReport(String type, Integer id, LocalDate dateFrom, LocalDate dateTo);

    TransactionDetailReportResponse getCategoryReportDetail( Integer id, LocalDate dateFrom, LocalDate dateTo);
}