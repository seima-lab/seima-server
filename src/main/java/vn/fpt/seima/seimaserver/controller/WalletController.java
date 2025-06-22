package vn.fpt.seima.seimaserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.wallet.CreateWalletRequest;
import vn.fpt.seima.seimaserver.dto.response.wallet.WalletResponse;
import vn.fpt.seima.seimaserver.service.WalletService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "wallet", description = "Wallet management APIs")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new wallet for the current user")
    public ApiResponse<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        WalletResponse wallet = walletService.createWallet(request);
        return ApiResponse.<WalletResponse>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Wallet created successfully")
                .data(wallet)
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get the current user's wallet by ID")
    public ApiResponse<WalletResponse> getWallet(@PathVariable Integer id) {
        WalletResponse wallet = walletService.getWallet(id);
        return ApiResponse.<WalletResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Wallet retrieved successfully")
                .data(wallet)
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all wallets for the current user")
    public ApiResponse<List<WalletResponse>> getAllWallets() {
        List<WalletResponse> wallets = walletService.getAllWallets();
        return ApiResponse.<List<WalletResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Wallets retrieved successfully")
                .data(wallets)
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update the current user's wallet")
    public ApiResponse<WalletResponse> updateWallet(
            @PathVariable Integer id,
            @Valid @RequestBody CreateWalletRequest request) {
        WalletResponse wallet = walletService.updateWallet(id, request);
        return ApiResponse.<WalletResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Wallet updated successfully")
                .data(wallet)
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete the current user's wallet")
    public ApiResponse<Void> deleteWallet(@PathVariable Integer id) {
        walletService.deleteWallet(id);
        return ApiResponse.<Void>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Wallet deleted successfully")
                .build();
    }
} 