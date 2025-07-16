# Frontend API Flow Guide - Owner Exit Group

## 🎯 Quick Overview

**ADMIN/MEMBER**: Exit immediately ✅  
**OWNER**: Must transfer ownership OR delete group first ⚠️

## 📞 API Calls Flow

### **Step 1: Initial Exit Attempt**
```typescript
// All roles call this first
POST /api/v1/group-members/group/{groupId}/exit

// Response:
// - ADMIN/MEMBER: 200 OK (exit success)
// - OWNER: 400 Error "must transfer ownership or delete the group"
```

### **Step 2: For OWNER - Get Exit Options**
```typescript
// When step 1 fails for OWNER, call this:
GET /api/v1/group-members/group/{groupId}/owner-exit-options

// Response:
{
  "canTransferOwnership": true,
  "canDeleteGroup": true,
  "eligibleMembersCount": 3,
  "message": "You have 3 eligible member(s) to transfer ownership to, or you can delete the group."
}
```

### **Step 3A: Transfer Ownership Flow**
```typescript
// 3A.1 - Get eligible members
GET /api/v1/group-members/group/{groupId}/eligible-for-ownership

// 3A.2 - User selects a member, then transfer
POST /api/v1/group-members/group/{groupId}/transfer-ownership
{
  "newOwnerUserId": 456
}

// 3A.3 - After transfer success, call exit again
POST /api/v1/group-members/group/{groupId}/exit
// Now works because user is MEMBER
```

### **Step 3B: Delete Group Flow**
```typescript
// User confirms deletion
DELETE /api/v1/groups/{groupId}
// Group deleted, all members removed
```

## 🔄 Complete Frontend Flow

```typescript
const handleExitGroup = async (groupId) => {
  try {
    // Try to exit
    await exitGroupAPI(groupId);
    showSuccess("Exited successfully");
    
  } catch (error) {
    if (error.message.includes("transfer ownership or delete")) {
      // OWNER case - show options
      const options = await getOwnerExitOptionsAPI(groupId);
      showOwnerExitPopup(options);
    } else {
      showError(error.message);
    }
  }
};

const showOwnerExitPopup = (options) => {
  // Show popup with 2 buttons:
  // 1. "Transfer Ownership" (if canTransferOwnership)
  // 2. "Delete Group"
};

const handleTransferOwnership = async (groupId) => {
  const members = await getEligibleMembersAPI(groupId);
  const selectedMember = await showMemberSelection(members);
  
  await transferOwnershipAPI(groupId, selectedMember.userId);
  await exitGroupAPI(groupId); // Now works
  showSuccess("Ownership transferred and exited");
};

const handleDeleteGroup = async (groupId) => {
  const confirmed = await showConfirmation("Delete group permanently?");
  if (confirmed) {
    await deleteGroupAPI(groupId);
    showSuccess("Group deleted");
  }
};
```

## 📋 Error Handling

```typescript
// Common errors to handle:
// - "Group not found" (404)
// - "Only group owner can..." (403) 
// - "No eligible members found" (400)
// - "Selected user is not active" (400)
```

## 🎨 UI Components Needed

1. **Owner Exit Options Popup**
   - Title: "You are the Group Owner"
   - Message from API response
   - Buttons: "Transfer Ownership", "Delete Group", "Cancel"

2. **Member Selection Popup**
   - List of eligible members from API
   - Select one member
   - "Confirm Transfer" button

3. **Delete Confirmation Dialog**
   - Warning message
   - "Delete" and "Cancel" buttons

## 🔗 API Summary

| Action | Method | Endpoint |
|--------|--------|----------|
| Exit Group | POST | `/api/v1/group-members/group/{groupId}/exit` |
| Get Options | GET | `/api/v1/group-members/group/{groupId}/owner-exit-options` |
| Get Members | GET | `/api/v1/group-members/group/{groupId}/eligible-for-ownership` |
| Transfer | POST | `/api/v1/group-members/group/{groupId}/transfer-ownership` |
| Delete Group | DELETE | `/api/v1/groups/{groupId}` |

All APIs require **Authorization: Bearer {token}** 