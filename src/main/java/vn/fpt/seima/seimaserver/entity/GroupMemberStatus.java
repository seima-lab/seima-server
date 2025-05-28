package vn.fpt.seima.seimaserver.entity;

public enum GroupMemberStatus {
    PENDING_APPROVAL,
    APPROVED,
    INVITED,
    REJECTED,
    LEFT,
    ACTIVE // Trạng thái chung sau khi approved/invited và chưa left
}
