package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.request.group.UpdateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupDetailResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.dto.response.group.UserJoinedGroupResponse;
import vn.fpt.seima.seimaserver.exception.GroupException;

import java.util.List;

// import vn.fpt.seima.seimaserver.entity.Group;
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

    /**
     * Updates group information (name and avatar) by group admin only.
     * Only admin members of the group can perform this operation.
     *
     * @param groupId the ID of the group to update
     * @param request the request containing updated group information
     * @return the updated group response with new information
     * @throws GroupException if user is not admin or group not found
     */
    GroupResponse updateGroupInformation(Integer groupId, UpdateGroupRequest request);

    /**
     * Gets all groups that the current user has joined.
     * Only returns active groups where user has active membership.
     *
     * @return list of groups the current user has joined, ordered by join date desc
     */
    List<UserJoinedGroupResponse> getUserJoinedGroups();

    /**
     * Archives a group (soft delete) by setting groupIsActive to false.
     * Only admin members of the group can perform this operation.
     * This action cannot be undone and will make the group inaccessible to all members.
     *
     * @param groupId the ID of the group to archive
     * @return the updated group response with groupIsActive set to false
     * @throws GroupException if user is not admin, group not found, or group already archived
     */
    GroupResponse archiveGroup(Integer groupId);
}