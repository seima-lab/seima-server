# Hướng Dẫn Mời Thành Viên Vào Nhóm

## Mô Tả

Tài liệu này mô tả quy trình mời người dùng tham gia nhóm thông qua liên kết mời (invitation link) sử dụng Firebase Dynamic Links.

## Quy Trình Mời Thành Viên

```mermaid
sequenceDiagram
    participant UserA as Người Mời (Trong App)
    participant RNApp as React Native App
    participant SpringBoot as Spring Boot Backend
    participant Firebase as Firebase Dynamic Links
    participant UserB as Người Được Mời

    UserA->>RNApp: Mở chi tiết nhóm & Lấy link mời
    RNApp->>UserA: Hiển thị link (đã tạo sẵn)
    UserA->>UserB: Gửi link mời

    UserB->>Firebase: Bấm vào link (seima.app.com/invite/...)
    alt App đã được cài
        Firebase->>RNApp: Mở App qua Deep Link
    else App chưa được cài
        Firebase->>UserB: Điều hướng tới App/Play Store
        UserB->>RNApp: Tải và mở App lần đầu
        Firebase->>RNApp: Trả về link gốc (Deferred Deep Link)
    end

    RNApp->>SpringBoot: GET /api/invites/{code}/details (Lấy T.tin nhóm)
    SpringBoot->>RNApp: Trả về {groupName, memberCount...}
    RNApp->>UserB: Hiển thị màn hình xác nhận tham gia

    UserB->>RNApp: Bấm nút "Tham Gia"
    RNApp->>SpringBoot: POST /api/groups/join (Gửi code + token user)
    SpringBoot->>SpringBoot: Xử lý: thêm user vào nhóm
    SpringBoot-->>RNApp: Phản hồi thành công
    RNApp->>UserB: Điều hướng vào màn hình nhóm

    Note over SpringBoot: Gửi thông báo bất đồng bộ (Async)
    SpringBoot->>Firebase: Gửi Push Notification tới Admin
```

## Các Bước Chính

1. **Tạo Link Mời**: Người dùng trong app tạo link mời cho nhóm
2. **Chia Sẻ Link**: Gửi link mời cho người khác
3. **Xử Lý Deep Link**: Firebase Dynamic Links xử lý việc mở app hoặc tải app
4. **Lấy Thông Tin Nhóm**: App gọi API để lấy thông tin chi tiết nhóm
5. **Xác Nhận Tham Gia**: Người được mời xác nhận tham gia nhóm
6. **Thêm Vào Nhóm**: Backend xử lý thêm user vào nhóm
7. **Thông Báo**: Gửi thông báo cho admin về thành viên mới

## API Endpoints

- `GET /api/invites/{code}/details` - Lấy thông tin chi tiết về lời mời
- `POST /api/groups/join` - Tham gia nhóm bằng mã mời

## Công Nghệ Sử Dụng

- **React Native**: Ứng dụng mobile
- **Spring Boot**: Backend API
- **Firebase Dynamic Links**: Xử lý deep linking
- **Firebase Cloud Messaging**: Push notifications