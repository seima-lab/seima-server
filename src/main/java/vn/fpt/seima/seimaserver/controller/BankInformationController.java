package vn.fpt.seima.seimaserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.response.bank.BankInformationResponse;
import vn.fpt.seima.seimaserver.service.BankInformationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
@Tag(name = "bank", description = "Bank information APIs")
public class BankInformationController {

    private final BankInformationService bankInformationService;

    @GetMapping
    @Operation(summary = "Get all banks")
    public ApiResponse<List<BankInformationResponse>> getAllBanks() {
        List<BankInformationResponse> banks = bankInformationService.getAllBanks();
        return ApiResponse.<List<BankInformationResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Banks retrieved successfully")
                .data(banks)
                .build();
    }
} 