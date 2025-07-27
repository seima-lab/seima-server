# OWNER EXIT GROUP FLOW - Implementation Guide

## 🎯 Overview

This guide documents the new flow implementation for GROUP OWNER exit functionality. The new system provides a safe and structured way for group owners to leave groups while ensuring group continuity or proper dissolution.

## 📋 Business Requirements Implemented

### **Role-based Exit Rules:**
- **OWNER**: Must transfer ownership or delete group before leaving
- **ADMIN**: Can exit immediately 
- **MEMBER**: Can exit immediately

### **Owner Exit Options:**
1. **Transfer Ownership**: Promote another active member to owner, then exit as member
2. **Delete Group**: Soft delete the group and remove all members

## 🔄 API Implementation

### **New APIs Added:**

#### 1. **Enhanced Exit Group API**
- **Endpoint**: `POST /api/v1/group-members/group/{groupId}/exit`
- **Change**: Now returns specific error message for OWNER role
- **OWNER Response**: `400 Bad Request` with message: *"As group owner, you must transfer ownership or delete the group before leaving."*

#### 2. **Get Owner Exit Options**
- **Endpoint**: `GET /api/v1/group-members/group/{groupId}/owner-exit-options`
- **Purpose**: Returns available options for owner exit
- **Response**: `OwnerExitOptionsResponse`
```json
{
  "statusCode": 200,
  "message": "Owner exit options retrieved successfully",
  "data": {
    "groupId": 123,
    "groupName": "Sample Group",
    "canTransferOwnership": true,
    "canDeleteGroup": true,
    "eligibleMembersCount": 5,
    "message": "You have 5 eligible member(s) to transfer ownership to, or you can delete the group."
  }
}
```

#### 3. **Get Eligible Members for Ownership**
- **Endpoint**: `GET /api/v1/group-members/group/{groupId}/eligible-for-ownership`
- **Purpose**: Returns list of members eligible to become owner
- **Response**: `GroupMemberListResponse` (excluding current owner)

#### 4. **Transfer Ownership**
- **Endpoint**: `POST /api/v1/group-members/group/{groupId}/transfer-ownership`
- **Purpose**: Transfer group ownership to another member
- **Request Body**:
```json
{
  "newOwnerUserId": 456
}
```

#### 5. **Delete Group**
- **Endpoint**: `DELETE /api/v1/groups/{groupId}`
- **Purpose**: Soft delete group and remove all members
- **Implementation**: Sets `groupIsActive = false` and all members to `LEFT` status

## 🎨 Frontend Implementation Flow

### **Step 1: Initial Exit Attempt**
```typescript
// User clicks "Exit Group"
const handleExitGroup = async (groupId: number) => {
  try {
    await exitGroupAPI(groupId);
    // Success for ADMIN/MEMBER
    showSuccessMessage("Successfully exited the group");
    navigateToGroupList();
  } catch (error) {
    if (error.message.includes("transfer ownership or delete")) {
      // OWNER case - show options popup
      showOwnerExitOptionsPopup(groupId);
    } else {
      showErrorMessage(error.message);
    }
  }
};
```

### **Step 2: Show Owner Exit Options**
```typescript
const showOwnerExitOptionsPopup = async (groupId: number) => {
  const options = await getOwnerExitOptionsAPI(groupId);
  
  const popup = {
    title: "You are the Group Owner",
    message: options.message,
    buttons: [
      options.canTransferOwnership && {
        text: "Transfer Ownership",
        action: () => showTransferOwnershipFlow(groupId)
      },
      {
        text: "Delete Group",
        action: () => showDeleteGroupConfirmation(groupId)
      },
      {
        text: "Cancel",
        action: () => closePopup()
      }
    ].filter(Boolean)
  };
  
  showPopup(popup);
};
```

### **Step 3A: Transfer Ownership Flow**
```typescript
const showTransferOwnershipFlow = async (groupId: number) => {
  // Get eligible members
  const eligibleMembers = await getEligibleMembersAPI(groupId);
  
  // Show member selection popup
  const selectedMember = await showMemberSelectionPopup(eligibleMembers);
  
  if (selectedMember) {
    try {
      // Transfer ownership
      await transferOwnershipAPI(groupId, selectedMember.userId);
      showSuccessMessage(`Ownership transferred to ${selectedMember.userFullName}`);
      
      // Now exit as member
      await exitGroupAPI(groupId);
      showSuccessMessage("You have left the group");
      navigateToGroupList();
    } catch (error) {
      showErrorMessage(error.message);
    }
  }
};
```

### **Step 3B: Delete Group Flow**
```typescript
const showDeleteGroupConfirmation = async (groupId: number) => {
  const confirmed = await showConfirmationDialog({
    title: "Delete Group",
    message: "This action will permanently delete the group and remove all members. Are you sure?",
    confirmText: "Delete",
    cancelText: "Cancel"
  });
  
  if (confirmed) {
    try {
      await deleteGroupAPI(groupId);
      showSuccessMessage("Group deleted successfully");
      navigateToGroupList();
    } catch (error) {
      showErrorMessage(error.message);
    }
  }
};
```

## 🔒 Security & Validation

### **Backend Validations:**
1. **Authentication**: All APIs require valid JWT token
2. **Authorization**: 
   - Only OWNER can transfer ownership
   - Only OWNER can delete group
   - Only group members can exit
3. **Business Rules**:
   - Cannot transfer to inactive users
   - Cannot transfer to non-members
   - Cannot delete already inactive groups
   - Cannot exit inactive groups

### **Data Integrity:**
- **Soft Delete**: Groups are soft deleted (set inactive) to preserve data
- **Member Status**: All members set to `LEFT` when group is deleted
- **Ownership Transfer**: Atomic operation (old owner → member, new member → owner)

## 🧪 Testing Scenarios

### **Test Cases:**

1. **ADMIN/MEMBER Exit**: Should work immediately
2. **OWNER Exit with Eligible Members**: Should show both options
3. **OWNER Exit without Eligible Members**: Should show only delete option
4. **Transfer Ownership Success**: Should complete full flow
5. **Delete Group Success**: Should soft delete and remove all members
6. **Permission Validations**: Should block unauthorized actions
7. **Data Integrity**: Should maintain consistent state throughout operations

### **Edge Cases:**
- Only owner in group → Only delete option available
- Target user becomes inactive during transfer → Should fail with error
- Group becomes inactive during process → Should fail with error
- Concurrent ownership transfers → Should be handled by database constraints

## 📊 Database Changes

### **No Schema Changes Required**
- Reuses existing `GroupMemberRole` and `GroupMemberStatus` enums
- Leverages existing soft delete pattern for groups
- Uses existing member status transitions

### **Key Operations:**
1. **Transfer Ownership**: 
   ```sql
   UPDATE group_members SET role = 'MEMBER' WHERE user_id = ? AND group_id = ?;
   UPDATE group_members SET role = 'OWNER' WHERE user_id = ? AND group_id = ?;
   ```

2. **Delete Group**:
   ```sql
   UPDATE groups SET group_is_active = false WHERE group_id = ?;
   UPDATE group_members SET status = 'LEFT' WHERE group_id = ? AND status = 'ACTIVE';
   ```

## 🚀 Deployment Notes

### **Backward Compatibility:**
- Existing exit functionality for ADMIN/MEMBER unchanged
- New error message for OWNER provides clear guidance
- No breaking changes to existing APIs

### **Frontend Updates Required:**
1. Update error handling for exit API
2. Add new popup components for owner options
3. Implement member selection UI
4. Add confirmation dialogs
5. Update navigation flow after group deletion

## 📈 Benefits of New Implementation

1. **User Experience**: Clear guidance for owners on exit options
2. **Data Safety**: Prevents orphaned groups through required ownership transfer
3. **Flexibility**: Owners can choose between transfer and deletion
4. **Transparency**: Clear communication of available options
5. **Consistency**: Maintains existing patterns for other roles

## 🔍 Monitoring & Logging

### **Key Metrics to Track:**
- Owner exit attempts vs completions
- Transfer ownership vs delete group ratio
- Failed exit attempts and reasons
- Average time to complete owner exit flow

### **Log Events:**
- Owner exit attempt with options displayed
- Ownership transfer transactions
- Group deletion events
- Failed exit attempts with reasons

This implementation provides a comprehensive solution for safe group owner exits while maintaining system integrity and user experience. 