# GROUP MEMBER API FLOW DOCUMENTATION

## Base Information
- **Base URL**: `/api/v1/group-members`
- **Authentication**: Required (JWT Token via Authorization header)
- **Response Format**: All endpoints return data wrapped in `ApiResponse<T>` format

### Standard ApiResponse Structure
```json
{
  "statusCode": 200,
  "message": "Success message",
  "data": {
    // Response data object
  }
}
```

---

## 1. GET GROUP MEMBERS API

### **GET** `/api/v1/group-members/group/{groupId}`

**Purpose**: Lấy danh sách thành viên của group

**Path Parameters**:
- `groupId`: Integer (required)

**Permission Required**: User phải là member của group

**Response**: `ApiResponse<GroupMemberListResponse>`
```typescript
interface GroupMemberListResponse {
  groupId: number;
  groupName: string;
  groupAvatarUrl: string | null;
  totalMembersCount: number;
  groupLeader: GroupMemberResponse;
  members: GroupMemberResponse[];        // Excludes leader
  currentUserRole: "OWNER" | "ADMIN" | "MEMBER";
}

interface GroupMemberResponse {
  userId: number;
  userFullName: string;
  userAvatarUrl: string | null;
  role: "OWNER" | "ADMIN" | "MEMBER";
}
```

**Success Response**: `200 OK`
**Error Cases**:
- `400`: Group ID cannot be null
- `401`: Unauthorized
- `403`: You don't have permission to view this group's members
- `404`: Group not found

**Business Logic**:
1. Validate user is active member of group
2. Get group leader (OWNER) separately
3. Get all other active members (excluding leader)
4. Filter out inactive user accounts
5. Return organized member list with current user's role

**Frontend Implementation**:
```typescript
const getGroupMembers = async (groupId: number) => {
  const response = await fetch(`/api/v1/group-members/group/${groupId}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return response.json();
};
```

---

## 2. REMOVE MEMBER API

### **DELETE** `/api/v1/group-members/group/{groupId}/members/{memberUserId}`

**Purpose**: Remove thành viên khỏi group

**Path Parameters**:
- `groupId`: Integer (required)
- `memberUserId`: Integer (required) - User ID của member cần remove

**Permission Required**: ADMIN hoặc OWNER

**Response**: `ApiResponse<null>`
```typescript
interface RemoveMemberResponse {
  statusCode: 200;
  message: "Member removed from group successfully";
  data: null;
}
```

**Success Response**: `200 OK`
**Error Cases**:
- `400`: Group ID/Member User ID cannot be null
- `401`: Unauthorized
- `403`: Only group administrators and owners can remove members
- `403`: Insufficient permission to remove this member
- `403`: Cannot remove the last administrator
- `404`: Group not found, Member not found

**Permission Hierarchy**:
- **OWNER**: Can remove ADMIN and MEMBER
- **ADMIN**: Can only remove MEMBER  
- **MEMBER**: Cannot remove anyone
- **Special Rules**:
  - Cannot remove OWNER
  - Cannot remove last ADMIN if no OWNER exists
  - Cannot remove inactive user accounts

**Frontend Implementation**:
```typescript
const removeMember = async (groupId: number, memberUserId: number) => {
  const response = await fetch(`/api/v1/group-members/group/${groupId}/members/${memberUserId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return response.json();
};
```

---

## 3. UPDATE MEMBER ROLE API

### **PUT** `/api/v1/group-members/group/{groupId}/members/{memberUserId}/role`

**Purpose**: Cập nhật role của thành viên (chỉ OWNER có quyền)

**Path Parameters**:
- `groupId`: Integer (required)
- `memberUserId`: Integer (required) - User ID của member cần update role

**Request Body**:
```typescript
interface UpdateMemberRoleRequest {
  newRole: "ADMIN" | "MEMBER";  // Required, cannot be OWNER
}
```

**Permission Required**: Chỉ OWNER

**Response**: `ApiResponse<null>`
```typescript
interface UpdateRoleResponse {
  statusCode: 200;
  message: "Member role updated successfully";
  data: null;
}
```

**Success Response**: `200 OK`
**Error Cases**:
- `400`: Group ID/Member User ID/New role cannot be null
- `401`: Unauthorized
- `403`: Only group owner can update member roles
- `403`: Cannot update your own role
- `403`: Cannot change owner role
- `403`: Cannot promote to owner (prevent multiple owners)
- `404`: Group not found, Member not found

**Business Rules**:
- Only OWNER can update member roles
- Cannot update own role
- Cannot change OWNER role
- Cannot promote anyone to OWNER (prevents multiple owners)
- Can only promote/demote between ADMIN and MEMBER
- Member already has this role (no change needed)

**Frontend Implementation**:
```typescript
const updateMemberRole = async (groupId: number, memberUserId: number, newRole: string) => {
  const response = await fetch(`/api/v1/group-members/group/${groupId}/members/${memberUserId}/role`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ newRole })
  });
  return response.json();
};
```

---

## Error Handling

### Common Error Response Format:
```json
{
  "statusCode": 400,
  "message": "Error description",
  "data": null
}
```

### Error Types:
- **400 Bad Request**: Validation errors, invalid data
- **401 Unauthorized**: Missing or invalid JWT token
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Server-side errors

---

## Permission Matrix

| Action | OWNER | ADMIN | MEMBER |
|--------|-------|-------|---------|
| View Members | ✅ | ✅ | ✅ |
| Remove MEMBER | ✅ | ✅ | ❌ |
| Remove ADMIN | ✅ | ❌ | ❌ |
| Remove OWNER | ❌ | ❌ | ❌ |
| Update Role | ✅ | ❌ | ❌ |

---

## Business Logic Flow

### Get Members Flow:
1. Validate user is active member of group
2. Find group leader (OWNER) with active account
3. Get all active group members
4. Filter out inactive user accounts
5. Separate leader from members list
6. Include current user's role in response

### Remove Member Flow:
1. Validate current user has ADMIN/OWNER permission
2. Find target member to remove
3. Check permission hierarchy rules
4. Check special rules (last admin protection)
5. Set member status to LEFT
6. Return success response

### Update Role Flow:
1. Validate current user is OWNER
2. Find target member for role update
3. Validate business rules (cannot update own role, owner role, etc.)
4. Update member role
5. Return success response

---

## Role Hierarchy & Rules

### **Role Hierarchy**: `OWNER > ADMIN > MEMBER`

### **Key Business Rules**:

1. **Group Leadership**:
   - Only ONE OWNER per group (group creator)
   - Multiple ADMINs allowed
   - Unlimited MEMBERs

2. **Removal Rules**:
   - OWNER cannot be removed by anyone
   - ADMIN can only remove MEMBER
   - OWNER can remove ADMIN and MEMBER
   - Cannot remove last ADMIN if no OWNER exists

3. **Role Update Rules**:
   - Only OWNER can update roles
   - Cannot update OWNER role
   - Cannot promote to OWNER
   - Cannot update own role

4. **Permission Inheritance**:
   - Higher roles inherit lower role permissions
   - All members can view group member list

---

## Frontend Integration Notes

1. **Permission Checks**: Always check user's role before showing action buttons
2. **UI State Management**: Disable buttons based on permission matrix
3. **Confirmation Dialogs**: Show confirmation for destructive actions (remove member)
4. **Real-time Updates**: Refresh member list after successful actions
5. **Error Handling**: Display appropriate error messages for different scenarios
6. **Loading States**: Show loading indicators during API calls
7. **Role Indicators**: Display role badges/labels for each member
8. **Last Admin Protection**: Warn user when trying to remove last admin

---

## Sample Frontend Component Logic

```typescript
// Check if current user can remove a specific member
const canRemoveMember = (currentUserRole: string, targetRole: string) => {
  if (currentUserRole === 'OWNER') {
    return targetRole !== 'OWNER'; // Owner can remove ADMIN/MEMBER
  }
  if (currentUserRole === 'ADMIN') {
    return targetRole === 'MEMBER'; // Admin can only remove MEMBER
  }
  return false; // Member cannot remove anyone
};

// Check if current user can update roles
const canUpdateRole = (currentUserRole: string) => {
  return currentUserRole === 'OWNER';
};

// Handle member removal with confirmation
const handleRemoveMember = async (memberId: number, memberName: string) => {
  if (confirm(`Are you sure you want to remove ${memberName} from the group?`)) {
    try {
      await removeMember(groupId, memberId);
      // Refresh member list
      await refreshMemberList();
    } catch (error) {
      // Handle error
    }
  }
};
``` 