package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.transaction.CreateTransactionRequest;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOverviewResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionResponse;
import vn.fpt.seima.seimaserver.service.TransactionService;

import java.time.YearMonth;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/expense")
    public ApiResponse<TransactionResponse> recordExpense(@ModelAttribute  CreateTransactionRequest request) {
        try {
            TransactionResponse transactionCreated = transactionService.recordExpense(request);
            return new ApiResponse<>(HttpStatus.OK.value(), "Transaction created successfully", transactionCreated);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(400, ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(500, ex.getMessage(), null);
        }
    }

    @PostMapping(value = "/income", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TransactionResponse> recordInCome(@ModelAttribute  CreateTransactionRequest request) {
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
    public ApiResponse<TransactionResponse> updateTransaction(@ModelAttribute  CreateTransactionRequest request,
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

            TransactionOverviewResponse response = transactionService.getTransactionOverview(month);

            return new ApiResponse<>(HttpStatus.OK.value(), "Transaction get successfully", response);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(), null);
        }
    }
} 