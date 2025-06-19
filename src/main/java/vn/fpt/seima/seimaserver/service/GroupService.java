package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupDetailResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;

public interface GroupService {
    /**
     * Creates a new group with optional image upload.
     * If an image is provided in the request, it will be uploaded to cloud storage
     * and the URL will be saved as the group's avatar.
     * 
     * @param request the request containing group details and optional image
     * @return the created group response with image URL if uploaded
     */
    GroupResponse createGroupWithImage(CreateGroupRequest request);

    /**
     * Gets detailed information about a group including members and leader.
     * 
     * @param groupId the ID of the group to retrieve details for
     * @return the group detail response containing group info, leader, and members
     */
    GroupDetailResponse getGroupDetail(Integer groupId);
}