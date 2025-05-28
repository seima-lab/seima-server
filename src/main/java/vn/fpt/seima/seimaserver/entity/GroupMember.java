package vn.fpt.seima.seimaserver.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_member")
public class GroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_member_id") // Thêm PK cho bảng này
    private Integer groupMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50) // ERD là varchar(50)
    private GroupMemberRole role; // ADMIN, MEMBER

    @CreationTimestamp // Hoặc để là LocalDateTime và set thủ công
    @Column(name = "join_date", updatable = false) // ERD là timestamp
    private LocalDateTime joinDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50) // ERD là varchar(255), nên dùng ENUM
    private GroupMemberStatus status; // PENDING_APPROVAL, APPROVED, INVITED, REJECTED, LEFT


}
