package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.request.transaction.CreateTransactionRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOverviewResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionResponse;

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

    TransactionOverviewResponse getTransactionOverview(YearMonth month);

}