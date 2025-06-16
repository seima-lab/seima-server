package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.fpt.seima.seimaserver.config.base.ApiResponse;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
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
} 