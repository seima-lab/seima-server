package vn.fpt.seima.seimaserver.dto.response.group;

public enum ResultType {
    STATUS_CHANGE_TO_PENDING_APPROVAL,
    INVALID_OR_USED_TOKEN,
    GROUP_INACTIVE_OR_DELETED,
    ALREADY_PENDING_APPROVAL,
    ALREADY_ACTIVE_MEMBER,
    GROUP_FULL
}
