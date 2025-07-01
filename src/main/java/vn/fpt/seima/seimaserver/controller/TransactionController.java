package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.transaction.CreateTransactionRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOcrResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOverviewResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionReportResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionResponse;
import vn.fpt.seima.seimaserver.entity.User;
import vn.fpt.seima.seimaserver.service.OcrService;
import vn.fpt.seima.seimaserver.service.TransactionService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final OcrService ocrService;

    @PostMapping(value = "/expense")
    public ApiResponse<TransactionResponse> recordExpense(@RequestBody  CreateTransactionRequest request) {
        try {
            TransactionResponse transactionCreated = transactionService.recordExpense(request);
            return new ApiResponse<>(HttpStatus.OK.value(), "Transaction created successfully", transactionCreated);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(400, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, ex.getMessage(), null);
        }
    }

    @PostMapping(value = "/income")
    public ApiResponse<TransactionResponse> recordInCome(@RequestBody  CreateTransactionRequest request) {
        try {
            TransactionResponse transactionCreated = transactionService.recordIncome(request);
            return new ApiResponse<>(HttpStatus.OK.value(), "Transaction created successfully", transactionCreated);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(400, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, ex.getMessage(), null);
        }
    }

    @PutMapping("/update/{id}")
    public ApiResponse<TransactionResponse> updateTransaction(@RequestBody  CreateTransactionRequest request,
                                                              @PathVariable Integer id) {
        try {
            TransactionResponse updatedTransaction = transactionService.updateTransaction(id, request);
            return new ApiResponse<>(HttpStatus.OK.value(), "Transaction updated successfully", updatedTransaction);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(), null);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<TransactionResponse> deleteTransaction(@PathVariable Integer id) {
        try {

            transactionService.deleteTransaction(id);

            return new ApiResponse<>(HttpStatus.OK.value(), "Transaction deleted successfully", null);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(), null);
        }
    }

    @GetMapping("/overview")
    public ApiResponse<TransactionOverviewResponse> overviewTransaction(@RequestParam("month")
                                                                        @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        try {
            User currentUser = UserUtils.getCurrentUser();
            if (currentUser == null) {
                return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "You are not logged in", null);
            }
            TransactionOverviewResponse response = transactionService.getTransactionOverview(currentUser.getUserId(), month);

            return new ApiResponse<>(HttpStatus.OK.value(), "Transaction get successfully", response);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(), null);
        }
    }

    @PostMapping("/scan-invoice")
    public ApiResponse<TransactionOcrResponse> scanInvoice(@RequestBody MultipartFile file) {
        try {
            TransactionOcrResponse transactionCreated = ocrService.extractTextFromFile(file);
            return new ApiResponse<>(HttpStatus.OK.value(), "Scan invoice successfully", transactionCreated);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(400, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, ex.getMessage(), null);
        }
    }

    @GetMapping("/view-history-transactions")
    public ApiResponse<Page<TransactionResponse>> viewHistoryTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponse> transactions = transactionService.getAllTransaction(pageable);

            return new ApiResponse<>(HttpStatus.OK.value(), "Transaction list retrieved successfully", transactions);
        } catch (Exception ex) {
            return new ApiResponse<>(500, "An unexpected error occurred", null);
        }
    }

    @GetMapping("/view-history-transactions-group/{groupId}")
    public ApiResponse<Page<TransactionResponse>> viewHistoryTransactionsGroup(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable Integer groupId) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponse> transactions = transactionService.viewHistoryTransactionsGroup(pageable, groupId);

            return new ApiResponse<>(HttpStatus.OK.value(), "Transaction list retrieved successfully", transactions);
        } catch (Exception ex) {
            return new ApiResponse<>(500, ex.getMessage(), null);
        }
    }

    @GetMapping("/view-history-transactions-date")
    public ApiResponse<Page<TransactionResponse>> viewHistoryTransactionsGroup(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "start_date") LocalDate startDate,
            @RequestParam(value = "end_date") LocalDate endDate
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponse> transactions = transactionService.viewHistoryTransactionsDate(pageable, startDate,endDate);

            return new ApiResponse<>(HttpStatus.OK.value(), "Transaction list retrieved successfully", transactions);
        } catch (Exception ex) {
            return new ApiResponse<>(500, ex.getMessage(), null);
        }
    }

    @GetMapping("/view-report")
    public ApiResponse<TransactionReportResponse> viewReportTransactions(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(value = "startDate") LocalDate startDate,
            @RequestParam(value = "endDate") LocalDate endDate
    ) {
        try {
            TransactionReportResponse transactions = transactionService.getTransactionReport(categoryId,startDate,endDate);
            return new ApiResponse<>(HttpStatus.OK.value(), "Transaction list retrieved successfully", transactions);
        } catch (Exception ex) {
            return new ApiResponse<>(500, ex.getMessage(), null);
        }
    }
} 