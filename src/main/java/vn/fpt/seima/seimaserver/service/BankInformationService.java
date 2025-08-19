package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.response.bank.BankInformationResponse;

import java.util.List;

public interface BankInformationService {
    List<BankInformationResponse> getAllBanks();
} 