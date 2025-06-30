# Email Invitation Feature Guide

## Tổng quan

Chức năng **Email Invitation** cho phép thành viên nhóm (Admin và Owner) mời người khác tham gia nhóm thông qua email. Hệ thống sẽ kiểm tra tài khoản người được mời và gửi email chứa link tham gia nhóm.

## Quy trình hoạt động

### 1. Quy trình gửi lời mời
```
1. Admin/Owner nhập email người muốn mời
2. Hệ thống kiểm tra email có tồn tại trong hệ thống không
3. Nếu tồn tại → Gửi email chứa link mời
4. Nếu không tồn tại → Trả về thông báo "Tài khoản không tồn tại"
```

### 2. Quy trình nhận lời mời
```
1. Người được mời nhận email
2. Click vào link trong email
3. Đăng nhập vào ứng dụng (nếu chưa đăng nhập)
4. Tự động tham gia nhóm
```

## API Endpoints

### Send Email Invitation
```http
POST /api/v1/groups/invitations/email
Content-Type: application/json
Authorization: Bearer <access_token>

{
    "groupId": 1,
    "email": "user@example.com"
}
```

### Response - Thành công (User tồn tại)
```json
{
    "statusCode": 200,
    "message": "Invitation sent successfully",
    "data": {
        "groupId": 1,
        "groupName": "My Group",
        "invitedEmail": "user@example.com",
        "inviteLink": "https://app.seima.com/invite/abc123",
        "emailSent": true,
        "userExists": true,
        "message": "Invitation sent successfully"
    }
}
```

### Response - User không tồn tại
```json
{
    "statusCode": 404,
    "message": "Tài khoản không tồn tại. Người dùng cần đăng ký tài khoản trước khi có thể tham gia nhóm.",
    "data": {
        "groupId": 1,
        "groupName": "My Group",
        "invitedEmail": "user@example.com",
        "inviteLink": null,
        "emailSent": false,
        "userExists": false,
        "message": "Tài khoản không tồn tại. Người dùng cần đăng ký tài khoản trước khi có thể tham gia nhóm."
    }
}
```

## Validation Rules

### Request Validation
- `groupId`: Bắt buộc, phải là số nguyên
- `email`: Bắt buộc, định dạng email hợp lệ

### Business Logic Validation
1. **Group exists and active**: Nhóm phải tồn tại và đang hoạt động
2. **User permission**: Chỉ Admin và Owner mới có thể mời thành viên
3. **Target user exists**: Email phải thuộc về tài khoản đã đăng ký và active
4. **Not already member**: Người được mời chưa phải là thành viên của nhóm
5. **No pending invitation**: Người được mời chưa có lời mời đang chờ xử lý

## Email Template

Email sử dụng template HTML với các thông tin:
- Tên người mời
- Tên nhóm và avatar
- Số lượng thành viên hiện tại
- Link tham gia nhóm

### Template Variables
```javascript
{
    inviterName: "Tên người mời",
    groupName: "Tên nhóm",
    groupAvatarUrl: "URL avatar nhóm",
    memberCount: "Số thành viên",
    inviteLink: "Link tham gia",
    appName: "Seima Lab"
}
```

## Error Cases

### 1. Permission Errors
```json
{
    "statusCode": 403,
    "message": "You don't have permission to invite members to this group"
}
```

### 2. Group Not Found
```json
{
    "statusCode": 404,
    "message": "Group not found"
}
```

### 3. User Already Member
```json
{
    "statusCode": 400,
    "message": "User is already a member of this group"
}
```

### 4. Pending Invitation
```json
{
    "statusCode": 400,
    "message": "User already has a pending invitation to this group"
}
```

### 5. Email Service Error
```json
{
    "statusCode": 200,
    "message": "Failed to send invitation email",
    "data": {
        "emailSent": false,
        "userExists": true,
        // ... other fields
    }
}
```

## Implementation Details

### Service Layer Architecture
```
GroupInvitationController
    ↓
GroupInvitationService
    ↓
GroupInvitationServiceImpl
    ↓
[GroupRepository, UserRepository, GroupMemberRepository, EmailService]
```

### Dependencies
- **GroupPermissionService**: Kiểm tra quyền mời thành viên
- **EmailService**: Gửi email HTML template
- **UserUtils**: Lấy thông tin user hiện tại
- **AppProperties**: Cấu hình ứng dụng

### Database Queries
1. Tìm nhóm theo ID
2. Kiểm tra membership của user hiện tại
3. Tìm target user theo email
4. Kiểm tra membership của target user
5. Đếm số thành viên active

## Security Considerations

### 1. Authentication
- Cần JWT token hợp lệ để gọi API
- User phải đăng nhập và active

### 2. Authorization
- Chỉ Admin và Owner có thể mời thành viên
- Không thể mời vào nhóm mà mình không phải thành viên

### 3. Data Validation
- Validate email format
- Rate limiting (nếu cần)

### 4. Privacy
- Không tiết lộ thông tin về user không tồn tại
- Log invitation activities

## Testing

### Unit Tests
File: `GroupInvitationServiceTest.java`

Test cases bao gồm:
- ✅ Successful invitation sending
- ✅ User not exists scenario  
- ✅ Validation errors (null request, invalid email, etc.)
- ✅ Permission errors
- ✅ Business logic violations
- ✅ Email service failures
- ✅ Edge cases (user previously left group)

### Integration Tests
Recommended test scenarios:
1. End-to-end invitation flow
2. Email template rendering
3. Database transactions
4. Error handling

## Configuration

### Email Templates
- Location: `src/main/resources/templates/group-invitation.html`
- Subject configuration in `application-{env}.yaml`

### Application Properties
```yaml
app:
  email:
    group-invitation:
      html-template: "group-invitation"
      subject: "Seima - Lời Mời Tham Gia Nhóm"
```

## Usage Examples

### Frontend Integration
```javascript
// Send invitation
const sendInvitation = async (groupId, email) => {
    const response = await fetch('/api/v1/groups/invitations/email', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`
        },
        body: JSON.stringify({
            groupId,
            email
        })
    });
    
    const result = await response.json();
    
    if (result.data?.userExists) {
        if (result.data.emailSent) {
            showSuccess('Invitation sent successfully!');
        } else {
            showError('Failed to send email invitation');
        }
    } else {
        showWarning('User account does not exist. Please ask them to register first.');
    }
};
```

### Mobile App Integration
```dart
// Flutter example
Future<InvitationResult> sendEmailInvitation(
  int groupId, 
  String email
) async {
  final response = await http.post(
    Uri.parse('$baseUrl/api/v1/groups/invitations/email'),
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $accessToken',
    },
    body: jsonEncode({
      'groupId': groupId,
      'email': email,
    }),
  );
  
  final data = jsonDecode(response.body);
  return InvitationResult.fromJson(data);
}
```

## Monitoring và Logging

### Metrics to Track
- Invitation send rate
- Email delivery success rate
- User acceptance rate
- Failed invitations by reason

### Log Levels
- INFO: Successful invitations
- WARN: User not found attempts
- ERROR: Email service failures
- DEBUG: Validation failures

---

*Last updated: [Current Date]*  
*Version: 1.0* 