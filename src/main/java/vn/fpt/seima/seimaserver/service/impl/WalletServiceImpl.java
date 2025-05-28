package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.repository.WalletRepository;
import vn.fpt.seima.seimaserver.service.WalletService;

@Service
@AllArgsConstructor
public class WalletServiceImpl implements WalletService {
    private WalletRepository walletRepository;
} 