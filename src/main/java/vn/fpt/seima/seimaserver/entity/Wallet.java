package vn.fpt.seima.seimaserver.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter 
@Entity
@Table(name = "wallet")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id", nullable = false)
    private Integer id;

    @Size(max = 3)
    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "current_balance", precision = 18, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "initial_balance", precision = 18, scale = 2)
    private BigDecimal initialBalance;

    @Size(max = 512)
    @Column(name = "icon_url", length = 512)
    private String iconUrl;

    @Column(name = "wallet_created_at")
    private Instant walletCreatedAt;

    @Column(name = "wallet_is_archived")
    private Boolean walletIsArchived;

    @Size(max = 255)
    @NotNull
    @Column(name = "wallet_name", nullable = false)
    private String walletName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "exclude_from_total")
    private Boolean excludeFromTotal = false;

    @Size(max = 100)
    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_type_id", nullable = false)
    private WalletType walletType;



}