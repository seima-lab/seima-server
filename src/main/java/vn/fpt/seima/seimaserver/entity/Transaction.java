package vn.fpt.seima.seimaserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transaction")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false) // ERD là wallet_d, sửa thành wallet_id
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id") // Nullable
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 50) // ERD là varchar(255), nên dùng ENUM
    private TransactionType transactionType; // INCOME, EXPENSE, TRANSFER

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "transaction_date") // ERD là timestamp
    private LocalDateTime transactionDate;

    @Column(name = "description", columnDefinition = "TEXT") // ERD là varchar(255), TEXT cho mô tả dài hơn
    private String description;

    @Column(name = "receipt_image_url", length = 512) // ERD là receipt_image_url
    private String receiptImageUrl;

    @Column(name = "payee_payer_name", length = 255)
    private String payeePayerName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
