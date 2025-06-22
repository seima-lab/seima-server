package vn.fpt.seima.seimaserver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupDetailResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionResponse;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.service.GroupService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController {
    
    private final GroupService groupService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupResponse> createGroup(@ModelAttribute @Validated CreateGroupRequest request) {
        GroupResponse groupResponse = groupService.createGroupWithImage(request);
        return new ApiResponse<>(HttpStatus.CREATED.value(), "Group created successfully", groupResponse);
    }

    @GetMapping("/{groupId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GroupDetailResponse> getGroupDetail(@PathVariable Integer groupId) {
        GroupDetailResponse groupDetail = groupService.getGroupDetail(groupId);
        return new ApiResponse<>(HttpStatus.OK.value(), "Group detail retrieved successfully", groupDetail);
    }

    @GetMapping("/list-transactions/{id}")
    public ApiResponse<Page<TransactionResponse>> getAllTransactionsByGroup(
            @PathVariable Integer id,
            Pageable pageable
    ) {
        try {

            Page<TransactionResponse> groupResponse = groupService.getTransactionByGroup(pageable, id);
            return new ApiResponse<>(HttpStatus.OK.value(), "Get transactions successfully", groupResponse);
        } catch (ResourceNotFoundException ex) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", null);
        }

    }
} 