package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.entity.GroupMemberStatus;
import vn.fpt.seima.seimaserver.exception.GroupException;


public interface GroupValidationService {
    
    /**
     * Validate if user can join more groups
     * Business rule: Each user can join maximum 10 groups
     */
    void validateUserCanJoinMoreGroups(Integer userId);
    
    /**
     * Validate if group can accept more members
     * Business rule: Each group can have maximum 20 members
     */
    void validateGroupCanAcceptMoreMembers(Integer groupId);
    
    /**
     * Validate if user can join a specific group
     * Combines both user and group validation
     */
    void validateUserCanJoinGroup(Integer userId, Integer groupId);
    
    /**
     * Get the current number of active groups for a user
     */
    int getUserActiveGroupCount(Integer userId);
    
    /**
     * Get the current number of active members in a group
     */
    int getGroupActiveMemberCount(Integer groupId);
} 