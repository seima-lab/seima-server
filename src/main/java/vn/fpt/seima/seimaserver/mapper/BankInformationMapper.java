package vn.fpt.seima.seimaserver.mapper;

import org.mapstruct.*;
import vn.fpt.seima.seimaserver.dto.response.bank.BankInformationResponse;
import vn.fpt.seima.seimaserver.entity.BankInformation;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BankInformationMapper {
    BankInformationResponse toResponse(BankInformation bankInformation);
} 