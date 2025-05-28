package vn.fpt.seima.seimaserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@Table(name = "wallet")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Integer walletId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "wallet_name", length = 255, nullable = false)
    private String walletName;

    @Column(name = "current_balance", precision = 18, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "currency_code", length = 3) // ERD là current_code varchar(3)
    private String currencyCode; // Ví dụ: VND, USD

    @Column(name = "icon_url", length = 512)
    private String iconUrl;

    @CreationTimestamp
    @Column(name = "wallet_created_at", updatable = false) // ERD là wallet_created_at (timestamp)
    private LocalDateTime walletCreatedAt;

    @Column(name = "wallet_is_archived") // ERD là wallet_is_archived (bit)
    private Boolean walletIsArchived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_type_id", nullable = false)
    private WalletType walletType;

    @OneToMany(mappedBy = "wallet")
    private Set<Transaction> transactions;
}
