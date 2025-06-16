package vn.fpt.seima.seimaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionResponse;
import vn.fpt.seima.seimaserver.entity.Group;

public interface GroupService {
    GroupResponse createGroup(CreateGroupRequest request);

    Page<TransactionResponse> getTransactionByGroup(Pageable pageable, Integer groupId);

}