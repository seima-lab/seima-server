package vn.fpt.seima.seimaserver.entity;

public enum GroupMemberRole {
    OWNER,   // Group creator - highest authority
    ADMIN,   // Can manage members but cannot remove owner
    MEMBER   // Regular member
}
