package vn.fpt.seima.seimaserver.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.response.bank.BankInformationResponse;
import vn.fpt.seima.seimaserver.entity.BankInformation;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.BankInformationMapper;
import vn.fpt.seima.seimaserver.repository.BankInformationRepository;
import vn.fpt.seima.seimaserver.service.BankInformationService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankInformationServiceImpl implements BankInformationService {

    private final BankInformationRepository bankInformationRepository;
    private final BankInformationMapper bankInformationMapper;

    @Override
    public List<BankInformationResponse> getAllBanks() {
        return bankInformationRepository.findAll().stream()
                .map(bankInformationMapper::toResponse)
                .collect(Collectors.toList());
    }
} 