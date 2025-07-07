# Group Invitation API - Postman Collection

## Mô tả
Collection này chứa các test cases để test API Group Invitation của Seima Server.

## Import vào Postman

1. Mở Postman
2. Click **Import** 
3. Chọn file `Group_Invitation_API_Postman_Collection.json`
4. Click **Import**

## Cấu hình Environment

### Cách 1: Sử dụng Collection Variables
Collection đã có sẵn các variables, bạn chỉ cần cập nhật:

1. Click vào Collection name
2. Chọn tab **Variables**
3. Cập nhật các giá trị:
   - `baseUrl`: URL của server (mặc định: `http://localhost:8080`)
   - `accessToken`: JWT token để authentication
   - `testGroupId`: ID của group để test
   - `testInviteCode`: Invite code hợp lệ để test

### Cách 2: Tạo Environment mới
1. Click vào **Environments** trong sidebar
2. Click **Create Environment**
3. Thêm các variables:
   ```
   baseUrl: http://localhost:8080
   accessToken: your_jwt_token_here
   testGroupId: 1
   testInviteCode: test-invite-123
   ```

## Các API Endpoints

### 1. Email Invitation APIs
- **Send Email Invitation - Success**: Test gửi invitation thành công
- **Send Email Invitation - User Not Found**: Test khi user không tồn tại
- **Send Email Invitation - Invalid Email**: Test với email format không hợp lệ
- **Send Email Invitation - Missing Group ID**: Test validation khi thiếu groupId
- **Send Email Invitation - Group Not Found**: Test khi group không tồn tại
- **Send Email Invitation - No Authorization**: Test khi không có JWT token

### 2. Invitation Details APIs
- **Get Invitation Details - Valid Code**: Lấy thông tin invitation với code hợp lệ
- **Get Invitation Details - Invalid Code**: Test với invite code không hợp lệ

### 3. Join Group APIs
- **Join Group - Valid Invite Code**: Join group với invite code hợp lệ
- **Join Group - Invalid Invite Code**: Test với invite code không hợp lệ

### 4. Validate Invitation APIs
- **Validate Invitation - Valid Code**: Validate invite code hợp lệ
- **Validate Invitation - Invalid Code**: Validate invite code không hợp lệ

## Cách lấy JWT Token

### 1. Sử dụng Login API
```bash
POST /api/v1/auth/login
{
    "email": "your_email@example.com",
    "password": "your_password"
}
```

### 2. Copy token từ response
```json
{
    "status": 200,
    "message": "Login successful",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "...",
        "user": {...}
    }
}
```

### 3. Set vào Collection Variable
- Copy `accessToken` từ response
- Paste vào variable `accessToken` trong collection

## Test Data Setup

### Tạo test data
1. **Tạo user account** (nếu chưa có)
2. **Tạo group** và lấy `groupId`
3. **Lấy invite code** của group
4. **Update variables** trong collection

### Test scenarios
1. **Happy path**: User tồn tại, có permission, group active
2. **Error cases**: 
   - User không tồn tại
   - Không có permission
   - Group không tồn tại/inactive
   - Validation errors

## Expected Results

### Success Response
```json
{
    "status": 200,
    "message": "Invitation sent successfully",
    "data": {
        "groupId": 1,
        "groupName": "Test Group",
        "invitedEmail": "user@example.com",
        "inviteLink": "https://seima.app.com/test-invite-123",
        "emailSent": true,
        "userExists": true,
        "message": "Invitation sent successfully"
    }
}
```

### Error Response
```json
{
    "status": 500,
    "message": "Error message description",
    "data": null
}
```

## Automated Tests

Collection bao gồm các automated tests:
- **Response time**: < 5 giây
- **Content-Type**: application/json
- **Status codes**: Kiểm tra status code phù hợp
- **Response structure**: Validate cấu trúc response
- **Business logic**: Kiểm tra logic nghiệp vụ

## Chạy Collection

### Chạy toàn bộ collection
1. Click vào Collection name
2. Click **Run collection**
3. Chọn các requests muốn chạy
4. Click **Run**

### Chạy từng request
1. Click vào request cụ thể
2. Click **Send**
3. Xem kết quả trong **Tests** tab

## Troubleshooting

### Common Issues

1. **401/403 Unauthorized**
   - Kiểm tra JWT token có hợp lệ không
   - Token có hết hạn không
   - User có quyền truy cập API không

2. **500 Internal Server Error**
   - Kiểm tra server có đang chạy không
   - Kiểm tra database connection
   - Xem server logs để debug

3. **404 Not Found**
   - Kiểm tra URL có đúng không
   - Kiểm tra groupId/inviteCode có tồn tại không

4. **Validation Errors**
   - Kiểm tra request body format
   - Kiểm tra required fields
   - Kiểm tra data types

### Debug Tips
- Xem **Console** trong Postman để debug scripts
- Kiểm tra **Headers** tab để xem request/response headers
- Sử dụng **Pre-request Script** để log variables
- Kiểm tra server logs để debug backend issues 