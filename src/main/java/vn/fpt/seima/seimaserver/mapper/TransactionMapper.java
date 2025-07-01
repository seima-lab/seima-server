package vn.fpt.seima.seimaserver.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import vn.fpt.seima.seimaserver.dto.request.budget.CreateBudgetRequest;
import vn.fpt.seima.seimaserver.dto.request.transaction.CreateTransactionRequest;
import vn.fpt.seima.seimaserver.dto.response.budget.BudgetResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOcrResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOverviewResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionReportResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionResponse;
import vn.fpt.seima.seimaserver.entity.Budget;
import vn.fpt.seima.seimaserver.entity.Transaction;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {

    Transaction toEntity(CreateTransactionRequest request);

    @Mapping(target = "walletId", source = "wallet.id")
    @Mapping(target = "categoryId", source = "category.categoryId")
    @Mapping(target = "groupId", source = "group.groupId")
    @Mapping(target = "userId", source = "user.userId")
    TransactionResponse toResponse(Transaction transaction);

    @Mapping(target = "transactionId", ignore = true)
    void updateTransactionFromDto(CreateTransactionRequest dto, @MappingTarget Transaction budget);


    @Mapping(target = "transactionId", source = "transactionId")
    @Mapping(target = "categoryName", source = "category.categoryName")
    @Mapping(target = "categoryIconUrl", source = "category.categoryIconUrl")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "transactionType", source = "transactionType")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "transactionDate", source = "transactionDate")
    TransactionOverviewResponse.TransactionItem toTransactionItem(Transaction transaction);


    @Mapping(target = "categoryName", source = "category.categoryName")
    @Mapping(target = "categoryIconUrl", source = "category.categoryIconUrl")
    @Mapping(target = "amount", source = "amount")
    TransactionReportResponse.ReportByCategory toTransactionReportByCategory(Transaction transaction);
}
