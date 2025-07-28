package vn.fpt.seima.seimaserver.entity;

/**
 * Enum defining notification types in the system
 * Ensures type safety and maintainability
 */
public enum NotificationType {
    
    // Group related notifications
    GROUP_JOIN_REQUEST("GROUP_JOIN_REQUEST", "Group join request"),
    GROUP_JOIN_APPROVED("GROUP_JOIN_APPROVED", "Group join request approved"),
    GROUP_JOIN_REJECTED("GROUP_JOIN_REJECTED", "Group join request rejected"),
    GROUP_INVITATION_RECEIVED("GROUP_INVITATION_RECEIVED", "Group invitation received"),
    GROUP_MEMBER_ADDED("GROUP_MEMBER_ADDED", "New member added to group"),
    GROUP_MEMBER_REMOVED("GROUP_MEMBER_REMOVED", "Member removed from group"),
    GROUP_ROLE_UPDATED("GROUP_ROLE_UPDATED", "Group role updated"),
    
    // Transaction related notifications
    TRANSACTION_CREATED("TRANSACTION_CREATED", "Transaction created"),
    TRANSACTION_UPDATED("TRANSACTION_UPDATED", "Transaction updated"),
    TRANSACTION_DELETED("TRANSACTION_DELETED", "Transaction deleted"),
    
    // Budget related notifications
    BUDGET_LIMIT_EXCEEDED("BUDGET_LIMIT_EXCEEDED", "Budget limit exceeded"),
    BUDGET_LIMIT_WARNING("BUDGET_LIMIT_WARNING", "Budget limit warning"),
    BUDGET_CREATED("BUDGET_CREATED", "Budget created"),
    BUDGET_UPDATED("BUDGET_UPDATED", "Budget updated"),
    
    // System notifications
    SYSTEM_MAINTENANCE("SYSTEM_MAINTENANCE", "System maintenance"),
    SYSTEM_UPDATE("SYSTEM_UPDATE", "System update"),
    SECURITY_ALERT("SECURITY_ALERT", "Security alert"),
    
    // User related notifications
    PASSWORD_CHANGED("PASSWORD_CHANGED", "Password changed"),
    EMAIL_VERIFIED("EMAIL_VERIFIED", "Email verified"),
    PROFILE_UPDATED("PROFILE_UPDATED", "Profile updated"),
    
    // Wallet related notifications
    WALLET_CREATED("WALLET_CREATED", "Wallet created"),
    WALLET_UPDATED("WALLET_UPDATED", "Wallet updated"),
    WALLET_DELETED("WALLET_DELETED", "Wallet deleted"),
    
    // General notifications
    GENERAL_INFO("GENERAL_INFO", "General information"),
    GENERAL_WARNING("GENERAL_WARNING", "General warning"),
    GENERAL_ERROR("GENERAL_ERROR", "General error"),

    // Financial Health
    FINANCIAL_HEALTH_LOW("FINANCIAL_HEALTH_LOW", "Financial health low");
    
    private final String code;
    private final String description;
    
    NotificationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get NotificationType from code string
     * @param code the notification type code
     * @return corresponding NotificationType
     */
    public static NotificationType fromCode(String code) {
        for (NotificationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown notification type code: " + code);
    }
    
    /**
     * Check if notification type is group-related
     * @return true if group-related notification
     */
    public boolean isGroupRelated() {
        return this == GROUP_JOIN_REQUEST || 
               this == GROUP_JOIN_APPROVED || 
               this == GROUP_JOIN_REJECTED ||
               this == GROUP_INVITATION_RECEIVED ||
               this == GROUP_MEMBER_ADDED ||
               this == GROUP_MEMBER_REMOVED ||
               this == GROUP_ROLE_UPDATED;
    }
    
    /**
     * Check if notification type is transaction-related
     * @return true if transaction-related notification
     */
    public boolean isTransactionRelated() {
        return this == TRANSACTION_CREATED || 
               this == TRANSACTION_UPDATED || 
               this == TRANSACTION_DELETED;
    }
    
    /**
     * Check if notification type is budget-related
     * @return true if budget-related notification
     */
    public boolean isBudgetRelated() {
        return this == BUDGET_LIMIT_EXCEEDED || 
               this == BUDGET_LIMIT_WARNING ||
               this == BUDGET_CREATED ||
               this == BUDGET_UPDATED;
    }
} 