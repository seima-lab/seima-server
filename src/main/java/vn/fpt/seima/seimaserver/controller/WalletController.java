package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.fpt.seima.seimaserver.service.WalletService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/wallets")
public class WalletController {
    private WalletService walletService;
} 