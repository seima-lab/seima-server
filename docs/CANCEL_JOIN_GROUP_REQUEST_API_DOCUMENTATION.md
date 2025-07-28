# Cancel Join Group Request API Documentation

## Overview
API để người dùng có thể hủy bỏ request join group khi đang trong trạng thái pending approval.

## Endpoint
```
POST /api/v1/groups/cancel-join
```

## Authentication
- **Required**: Bearer Token
- **Header**: `Authorization: Bearer <token>`

## Request

### Headers
```
Content-Type: application/json
Authorization: Bearer <token>
```

### Body
```json
{
  "groupId": 1
}
```

### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| groupId | Integer | Yes | ID của group muốn hủy request join |

## Response

### Success Response (200 OK)
```json
{
  "status": 200,
  "message": "Join group request canceled successfully",
  "data": null
}
```

### Error Response (400 Bad Request) - Validation Errors
```json
{
  "status": 400,
  "message": "Group ID cannot be null",
  "data": null
}
```

```json
{
  "status": 400,
  "message": "Group not found",
  "data": null
}
```

```json
{
  "status": 400,
  "message": "No pending join request found for this group",
  "data": null
}
```

### Error Response (401 Unauthorized)
```json
{
  "status": 401,
  "message": "Unauthorized",
  "data": null
}
```

### Error Response (500 Internal Server Error)
```json
{
  "status": 500,
  "message": "Unable to identify the current user",
  "data": null
}
```

## Business Logic

1. **Authentication**: Kiểm tra user đã đăng nhập
2. **Validation**: Validate input parameters
3. **Group Check**: Kiểm tra group tồn tại và đang active
4. **Membership Check**: Kiểm tra user có pending request trong group không
5. **Cancel**: Cập nhật status từ `PENDING_APPROVAL` thành `LEFT`

## Validation Rules

### Input Validation
- `groupId` không được null
- `groupId` phải là số nguyên dương (> 0)

### Business Validation
- Group phải tồn tại trong database
- Group phải đang active (`groupIsActive = true`)
- User phải có membership với status `PENDING_APPROVAL` trong group

## Use Cases

### Frontend Integration
```javascript
// Hủy request join group
const cancelJoinGroupRequest = async (groupId) => {
  try {
    const response = await fetch('/api/v1/groups/cancel-join', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ groupId })
    });
    
    const result = await response.json();
    
    if (response.ok) {
      return result; // Success response
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('Error canceling join request:', error);
    throw error;
  }
};

// Sử dụng trong component
const PendingGroupItem = ({ group }) => {
  const [canceling, setCanceling] = useState(false);
  
  const handleCancelRequest = async () => {
    if (window.confirm('Are you sure you want to cancel this join request?')) {
      setCanceling(true);
      try {
        await cancelJoinGroupRequest(group.groupId);
        // Refresh pending groups list or remove from UI
        onCancelSuccess(group.groupId);
      } catch (error) {
        alert('Failed to cancel request: ' + error.message);
      } finally {
        setCanceling(false);
      }
    }
  };
  
  return (
    <div className="pending-group-item">
      <h3>{group.groupName}</h3>
      <p>Requested: {new Date(group.requestedAt).toLocaleDateString()}</p>
      <p>Members: {group.activeMemberCount}</p>
      <button 
        onClick={handleCancelRequest} 
        disabled={canceling}
        className="cancel-button"
      >
        {canceling ? 'Canceling...' : 'Cancel Request'}
      </button>
    </div>
  );
};
```

## Testing

### Unit Tests Coverage

#### Normal Cases
- ✅ `cancelJoinGroupRequest_Success_WhenValidPendingRequest`
- ✅ `cancelJoinGroupRequest_Success_WhenGroupIdIsOne`
- ✅ `cancelJoinGroupRequest_Success_WhenGroupIdIsMaxInteger`

#### Abnormal Cases
- ✅ `cancelJoinGroupRequest_ThrowsException_WhenRequestIsNull`
- ✅ `cancelJoinGroupRequest_ThrowsException_WhenGroupIdIsNull`
- ✅ `cancelJoinGroupRequest_ThrowsException_WhenGroupIdIsZero`
- ✅ `cancelJoinGroupRequest_ThrowsException_WhenGroupIdIsNegative`
- ✅ `cancelJoinGroupRequest_ThrowsException_WhenUserNotFound`
- ✅ `cancelJoinGroupRequest_ThrowsException_WhenGroupNotFound`
- ✅ `cancelJoinGroupRequest_ThrowsException_WhenGroupIsInactive`
- ✅ `cancelJoinGroupRequest_ThrowsException_WhenNoPendingRequestFound`
- ✅ `cancelJoinGroupRequest_ThrowsException_WhenUserHasActiveMembership`

#### Boundary Cases
- ✅ `cancelJoinGroupRequest_Success_WhenGroupIdIsOne` (Minimum valid ID)
- ✅ `cancelJoinGroupRequest_Success_WhenGroupIdIsMaxInteger` (Maximum valid ID)

### Controller Tests
- ✅ Success case với valid request
- ✅ Validation errors (null, invalid JSON, missing fields)
- ✅ Business logic errors (group not found, no pending request)
- ✅ Boundary value testing

### Manual Testing Checklist
1. **Valid cancel request**: User có pending request → Cancel thành công
2. **No pending request**: User không có pending request → Error message
3. **Invalid group ID**: Group ID không tồn tại → Error message
4. **Inactive group**: Group đã bị archive → Error message
5. **Unauthorized**: Không có token → 401 error
6. **Invalid token**: Token không hợp lệ → 401 error
7. **Null request**: Request body null → Validation error
8. **Missing groupId**: Request thiếu groupId → Validation error

## Error Scenarios

### Common Error Messages
| Error Message | Cause | HTTP Status |
|---------------|-------|-------------|
| "Cancel request cannot be null" | Request body is null | 400 |
| "Group ID cannot be null" | groupId is null | 400 |
| "Group ID must be a positive integer" | groupId <= 0 | 400 |
| "Group not found" | Group doesn't exist or inactive | 400 |
| "No pending join request found for this group" | User has no pending request | 400 |
| "Unable to identify the current user" | User not authenticated | 500 |

## Related APIs
- `GET /api/v1/groups/pending` - Lấy danh sách pending groups
- `GET /api/v1/groups/joined` - Lấy danh sách group đã join
- `GET /api/v1/groups/{groupId}/my-status` - Kiểm tra status trong group

## Database Changes
- Cập nhật `group_member` table: thay đổi `status` từ `PENDING_APPROVAL` thành `LEFT`
- Không xóa record, chỉ update status để preserve history

## Security Considerations
- Chỉ user đã đăng nhập mới có thể cancel request
- User chỉ có thể cancel request của chính mình
- Validation đầy đủ input parameters
- Logging cho audit trail

## Performance Considerations
- Sử dụng transaction để đảm bảo data consistency
- Index trên `user_id`, `group_id`, `status` columns
- Minimal database queries (1 select + 1 update)

## Notes
- API này chỉ hoạt động với các request đang trong trạng thái `PENDING_APPROVAL`
- Sau khi cancel, user có thể request join lại group đó
- Không gửi notification cho admin/owner khi user cancel request
- Status được set thành `LEFT` thay vì xóa record để preserve history 