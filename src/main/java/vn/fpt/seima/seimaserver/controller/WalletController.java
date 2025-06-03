package vn.fpt.seima.seimaserver.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.dto.wallet.request.CreateWalletRequest;
import vn.fpt.seima.seimaserver.dto.wallet.response.WalletResponse;
import vn.fpt.seima.seimaserver.service.WalletService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Wallet management APIs")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Create a new wallet")
    public ResponseEntity<WalletResponse> createWallet(@RequestBody CreateWalletRequest request) {
        return ResponseEntity.ok(walletService.createWallet(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a wallet by ID")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Integer id) {
        return ResponseEntity.ok(walletService.getWallet(id));
    }

    @GetMapping
    @Operation(summary = "Get all wallets")
    public ResponseEntity<List<WalletResponse>> getAllWallets() {
        return ResponseEntity.ok(walletService.getAllWallets());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a wallet")
    public ResponseEntity<WalletResponse> updateWallet(
            @PathVariable Integer id,
            @RequestBody CreateWalletRequest request) {
        return ResponseEntity.ok(walletService.updateWallet(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a wallet")
    public ResponseEntity<Void> deleteWallet(@PathVariable Integer id) {
        walletService.deleteWallet(id);
        return ResponseEntity.noContent().build();
    }
} 