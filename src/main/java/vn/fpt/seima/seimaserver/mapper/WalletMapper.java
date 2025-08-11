package vn.fpt.seima.seimaserver.mapper;

import org.mapstruct.*;
import vn.fpt.seima.seimaserver.dto.request.wallet.CreateWalletRequest;
import vn.fpt.seima.seimaserver.dto.response.wallet.WalletResponse;
import vn.fpt.seima.seimaserver.entity.Wallet;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WalletMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "walletType", ignore = true)
    @Mapping(target = "walletCreatedAt", ignore = true)
    @Mapping(target = "walletIsArchived", ignore = true)
    @Mapping(target = "currentBalance", source = "balance")
    @Mapping(target = "bankName", source = "bankName")
    @Mapping(target = "iconUrl", source = "iconUrl")
    @Mapping(target = "initialBalance", source = "initialBalance")

    Wallet toEntity(CreateWalletRequest request);

    @Mapping(target = "walletTypeName", source = "walletType.typeName")
    @Mapping(target = "bankName", source = "bankName")
    @Mapping(target = "iconUrl", source = "iconUrl")
    @Mapping(target = "initialBalance", source = "initialBalance")
    WalletResponse toResponse(Wallet wallet);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "walletType", ignore = true)
    @Mapping(target = "walletCreatedAt", ignore = true)
    @Mapping(target = "walletIsArchived", ignore = true)
    @Mapping(target = "currentBalance", source = "balance")
    @Mapping(target = "bankName", source = "bankName")
    @Mapping(target = "iconUrl", source = "iconUrl")
    @Mapping(target = "initialBalance", source = "initialBalance")

    void updateEntity(@MappingTarget Wallet wallet, CreateWalletRequest request);
} 