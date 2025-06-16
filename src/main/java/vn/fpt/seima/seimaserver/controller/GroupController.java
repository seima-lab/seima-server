package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionResponse;
import vn.fpt.seima.seimaserver.entity.Group;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.service.GroupService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController {
    private final GroupService groupService;

    @PostMapping("/create")
    public ApiResponse<GroupResponse> createGroup(@RequestBody @Validated CreateGroupRequest request) {
        try {
            GroupResponse groupResponse = groupService.createGroup(request);
            return new ApiResponse<>(HttpStatus.CREATED.value(), "Group created successfully", groupResponse);
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", null);
        }
    }

    @GetMapping("/list-transaction/{id}")
    public ApiResponse<Page<TransactionResponse>> getAllTransactionsByGroup(
            @PathVariable Integer id,
            Pageable pageable
    ) {
        try {

            Page<TransactionResponse> groupResponse = groupService.getTransactionByGroup(pageable, id);
            return new ApiResponse<>(HttpStatus.OK.value(), "Group created successfully", groupResponse);
        } catch (ResourceNotFoundException ex) {
            return new ApiResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);
        } catch (Exception ex) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", null);
        }

    }
} 