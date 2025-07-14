# Notification Controller API - Postman Collection

## Giới thiệu

File này chứa Postman collection hoàn chỉnh cho tất cả API endpoints của **Notification Controller** trong Seima Server application. Collection được tổ chức theo folders chức năng với test cases đầy đủ cho cả success và error scenarios.

## Cách import vào Postman

1. Mở Postman
2. Click **Import** ở góc trên bên trái  
3. Chọn **Upload Files** hoặc kéo thả file `Notification_Controller_Postman_Collection.json`
4. Click **Import**

## Cấu trúc Collection

Collection được tổ chức thành **4 folders chính**:

### 📁 Get Notifications
- Get Notifications - Success with Default Pagination
- Get Notifications - Success with Custom Pagination  
- Get Notifications - Filter by Unread Status
- Get Notifications - Filter by Type
- Get Notifications - Filter by Date Range
- Get Notifications - Invalid Type Filter
- Get Notifications - No Authorization

### 📁 Unread Count
- Get Unread Count - Success
- Get Unread Count - No Authorization

### 📁 Mark as Read  
- Mark Single Notification as Read - Success
- Mark Single Notification as Read - Not Found
- Mark All Notifications as Read - Success
- Mark as Read - No Authorization

### 📁 Delete Notifications
- Delete Single Notification - Success
- Delete Single Notification - Not Found  
- Delete All Notifications - Success
- Delete Notifications - No Authorization

## Environment Variables

Collection sử dụng naming convention **camelCase** và auth config ở collection level:

| Biến | Mô tả | Giá trị mặc định |
|------|-------|------------------|
| `baseUrl` | URL gốc của server | `http://localhost:8080` |
| `accessToken` | JWT token để authentication | `your_jwt_token_here` |
| `testNotificationId` | ID của notification để test | `1` |

### Cách thiết lập Environment Variables:

1. Tạo Environment mới trong Postman
2. Thêm các biến trên với giá trị phù hợp cho môi trường test của bạn
3. Chọn Environment này khi chạy collection

## Authentication

Collection sử dụng **Bearer Token authentication** ở collection level:
- Auth được config tự động cho tất cả requests
- Sử dụng variable `{{accessToken}}`
- Một số requests có option "No Authorization" để test unauthorized access

## API Endpoints Chi Tiết

### 1. GET /api/v1/notifications
**Lấy danh sách notifications với phân trang và filter**

**Test Cases:**
- ✅ **Default Pagination**: Test với pagination mặc định
- ✅ **Custom Pagination**: Test với page=0&size=5
- ✅ **Filter by Read Status**: Test với isRead=false
- ✅ **Filter by Type**: Test với type=GROUP_INVITATION_RECEIVED
- ✅ **Filter by Date Range**: Test với startDate và endDate
- ❌ **Invalid Type**: Test với INVALID_TYPE (expect 400)
- ❌ **No Authorization**: Test không có token (expect 401)

**Query Parameters:**
- `page` (optional): Số trang (mặc định: 0)
- `size` (optional): Kích thước trang (mặc định: 10)  
- `isRead` (optional): Filter theo trạng thái đã đọc (true/false/null)
- `type` (optional): Filter theo loại notification
- `startDate` (optional): Filter từ ngày (ISO DateTime format)
- `endDate` (optional): Filter đến ngày (ISO DateTime format)

**Notification Types hỗ trợ:**
- `GROUP_JOIN_REQUEST`, `GROUP_JOIN_APPROVED`, `GROUP_JOIN_REJECTED`
- `GROUP_INVITATION_RECEIVED`, `GROUP_MEMBER_ADDED`, `GROUP_MEMBER_REMOVED`
- `GROUP_ROLE_UPDATED`, `TRANSACTION_CREATED`, `TRANSACTION_UPDATED`
- `TRANSACTION_DELETED`, `BUDGET_LIMIT_EXCEEDED`, `BUDGET_LIMIT_WARNING`
- `BUDGET_CREATED`, `BUDGET_UPDATED`, `SYSTEM_MAINTENANCE`
- `SYSTEM_UPDATE`, `SECURITY_ALERT`, `PASSWORD_CHANGED`
- `EMAIL_VERIFIED`, `PROFILE_UPDATED`, `WALLET_CREATED`
- `WALLET_UPDATED`, `WALLET_DELETED`, `GENERAL_INFO`
- `GENERAL_WARNING`, `GENERAL_ERROR`

### 2. GET /api/v1/notifications/unread-count
**Lấy số lượng notifications chưa đọc**

**Test Cases:**
- ✅ **Success**: Kiểm tra count >= 0
- ❌ **No Authorization**: Test unauthorized access

### 3. PUT /api/v1/notifications/{notificationId}/read
**Đánh dấu một notification là đã đọc**

**Test Cases:**
- ✅ **Success**: Đánh dấu thành công
- ❌ **Not Found**: Test với ID không tồn tại (999999)
- ❌ **No Authorization**: Test unauthorized access

### 4. PUT /api/v1/notifications/read-all
**Đánh dấu tất cả notifications là đã đọc**

**Test Cases:**
- ✅ **Success**: Return count của notifications được đánh dấu
- ❌ **No Authorization**: Test unauthorized access

### 5. DELETE /api/v1/notifications/{notificationId}
**Xóa một notification cụ thể**

**Test Cases:**
- ✅ **Success**: Xóa thành công
- ❌ **Not Found**: Test với ID không tồn tại
- ❌ **No Authorization**: Test unauthorized access

### 6. DELETE /api/v1/notifications/all
**Xóa tất cả notifications**

**Test Cases:**
- ✅ **Success**: Return count của notifications được xóa
- ❌ **No Authorization**: Test unauthorized access

## Advanced Testing Features

### Global Test Scripts
Collection bao gồm **global test scripts** chạy cho mỗi request:

- ✅ **Response Time**: Kiểm tra < 5000ms
- ✅ **Content Type**: Validate JSON response
- ✅ **API Structure**: Kiểm tra statusCode và message fields

### Pre-request Scripts
- Auto-set baseUrl nếu chưa có
- Generate random notification ID cho testing
- Dynamic variable management

### Individual Test Scripts
Mỗi request có test scripts riêng để:

- ✅ Validate status codes (200, 400, 401, 404)
- ✅ Check response structure và required fields
- ✅ Validate business logic (pagination, filters, counts)
- ✅ Test error messages và edge cases

## Response Format

Tất cả API endpoints trả về response theo format chuẩn:

```json
{
    "statusCode": 200,
    "message": "Success message",
    "data": {
        // Response data here
    }
}
```

**Pagination Response Example:**
```json
{
    "statusCode": 200,
    "message": "Notifications fetched successfully",
    "data": {
        "content": [...],
        "pageable": {...},
        "totalElements": 25,
        "totalPages": 3,
        "size": 10,
        "number": 0
    }
}
```

## Workflow Testing

### Recommended Test Sequence:
1. **Get Unread Count** → Check initial state
2. **Get Notifications** → Verify pagination và filters
3. **Mark as Read** → Test single notification
4. **Get Unread Count** → Verify count decreased
5. **Mark All as Read** → Bulk operation
6. **Delete Notifications** → Cleanup testing

### Error Scenario Testing:
- Test với invalid notification IDs
- Test authentication failures  
- Test invalid query parameters
- Test malformed requests

## Troubleshooting

**401 Unauthorized:**
- Kiểm tra `accessToken` variable có hợp lệ không
- Verify JWT token chưa expire
- Ensure collection-level auth được enable

**404 Not Found:**
- Kiểm tra `testNotificationId` có tồn tại không
- User có quyền access notification đó không
- Check notification không bị xóa trước đó

**400 Bad Request:**
- Validate query parameter format (đặc biệt startDate, endDate)  
- Check NotificationType spelling và case sensitivity
- Verify pagination parameters (page >= 0, size > 0)

**500 Internal Server Error:**
- Check server logs để debug
- Verify database connection
- Check service dependencies

## Performance Testing

Collection hỗ trợ performance testing với:
- Response time assertions (< 5000ms)
- Bulk operations testing (read-all, delete-all)
- Pagination performance với large datasets
- Filter performance với multiple conditions

## Version History

**v1.0.0** - Initial release with comprehensive test coverage
- ✅ All 6 API endpoints covered
- ✅ Success và error scenarios
- ✅ Global test scripts
- ✅ Folder organization
- ✅ Auth configuration 