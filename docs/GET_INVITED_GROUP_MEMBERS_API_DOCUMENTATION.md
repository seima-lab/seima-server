# Get Invited Group Members API Documentation

## Overview
API để lấy danh sách tất cả thành viên có trạng thái "invited" trong một group. Chỉ ADMIN và OWNER mới có quyền xem danh sách này.

## Endpoint
```
GET /api/v1/groups/{groupId}/invited-members
```

## Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| groupId | Integer | Yes | ID của group cần lấy danh sách invited members |

## Headers
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| Authorization | String | Yes | Bearer token (JWT) |

## Authentication
- Cần JWT token hợp lệ
- User phải đăng nhập và active
- User phải là thành viên active của group
- User phải có role ADMIN hoặc OWNER

## Response

### Success Response (200 OK)
```json
{
    "statusCode": 200,
    "message": "Invited members retrieved successfully",
    "data": [
        {
            "userId": 200,
            "userEmail": "invited1@example.com",
            "userFullName": "Invited User 1",
            "userAvatarUrl": "https://example.com/avatar1.jpg",
            "invitedAt": "2024-01-15T10:30:00",
            "assignedRole": "MEMBER"
        },
        {
            "userId": 300,
            "userEmail": "invited2@example.com",
            "userFullName": "Invited User 2",
            "userAvatarUrl": "https://example.com/avatar2.jpg",
            "invitedAt": "2024-01-15T09:15:00",
            "assignedRole": "MEMBER"
        }
    ]
}
```

### Empty Response (200 OK)
```json
{
    "statusCode": 200,
    "message": "Invited members retrieved successfully",
    "data": []
}
```

## Error Responses

### 400 Bad Request - Invalid Group ID
```json
{
    "statusCode": 400,
    "message": "Group ID must be a positive integer"
}
```

### 400 Bad Request - Null Group ID
```json
{
    "statusCode": 400,
    "message": "Group ID cannot be null"
}
```

### 404 Not Found - Group Not Found
```json
{
    "statusCode": 404,
    "message": "Group not found"
}
```

### 403 Forbidden - User Not Member
```json
{
    "statusCode": 403,
    "message": "You are not a member of this group"
}
```

### 403 Forbidden - Insufficient Permission
```json
{
    "statusCode": 403,
    "message": "You don't have permission to view invited members"
}
```

### 401 Unauthorized - Invalid Token
```json
{
    "statusCode": 401,
    "message": "Invalid or expired token"
}
```

## Response Fields

### InvitedGroupMemberResponse
| Field | Type | Description |
|-------|------|-------------|
| userId | Integer | ID của user được mời |
| userEmail | String | Email của user được mời |
| userFullName | String | Tên đầy đủ của user được mời |
| userAvatarUrl | String | URL avatar của user được mời |
| invitedAt | LocalDateTime | Thời gian gửi lời mời |
| assignedRole | String | Role sẽ được gán khi user accept invitation |

## Business Rules

### 1. Permission Requirements
- Chỉ ADMIN và OWNER mới có quyền xem danh sách invited members
- User phải là thành viên active của group

### 2. Group Validation
- Group phải tồn tại và active
- Group ID phải là số nguyên dương

### 3. User Validation
- User hiện tại phải đăng nhập và active
- User hiện tại phải là thành viên active của group

### 4. Data Filtering
- Chỉ trả về users có trạng thái INVITED
- Loại trừ user hiện tại khỏi danh sách
- Chỉ trả về users có tài khoản active

## Example Usage

### cURL
```bash
curl -X GET \
  'https://api.example.com/api/v1/groups/1/invited-members' \
  -H 'Authorization: Bearer YOUR_JWT_TOKEN' \
  -H 'Content-Type: application/json'
```

### JavaScript (Fetch)
```javascript
const response = await fetch('/api/v1/groups/1/invited-members', {
    method: 'GET',
    headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    }
});

const data = await response.json();
console.log(data);
```

### React Native (Axios)
```javascript
const getInvitedMembers = async (groupId) => {
    try {
        const response = await axios.get(
            `/api/v1/groups/${groupId}/invited-members`,
            {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            }
        );
        return response.data;
    } catch (error) {
        console.error('Error fetching invited members:', error);
        throw error;
    }
};
```

## Testing

### Unit Tests Coverage
- **N (Normal)**: Successful retrieval, empty list, different user roles
- **A (Abnormal)**: Invalid inputs, permission errors, group not found
- **B (Boundary)**: Minimum/maximum group IDs, edge cases

### Test Cases
1. ✅ Successfully get invited members (ADMIN role)
2. ✅ Successfully get invited members (OWNER role)
3. ✅ Empty list when no invited members
4. ✅ Group ID validation (null, zero, negative)
5. ✅ Group not found scenario
6. ✅ User not member scenario
7. ✅ Insufficient permission scenario
8. ✅ Boundary values (group ID = 1, Integer.MAX_VALUE)

## Related APIs
- `GET /api/v1/groups/{groupId}` - Get group details
- `GET /api/v1/groups/{groupId}/members` - Get active members
- `POST /api/v1/groups/{groupId}/invite` - Invite new members
- `GET /api/v1/groups/{groupId}/pending-members` - Get pending members

## Notes
- API này chỉ trả về thông tin cơ bản của invited users
- Không bao gồm thông tin nhạy cảm như phone number
- Thời gian invitedAt được lưu theo timezone của server
- Role assignedRole sẽ được gán khi user accept invitation 