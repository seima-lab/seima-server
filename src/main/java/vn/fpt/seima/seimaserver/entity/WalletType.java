package vn.fpt.seima.seimaserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "wallet_type")
@Getter
@Setter
public class WalletType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_type_id", nullable = false)
    private Long id;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Nullable, cho các type do người dùng định nghĩa
    private User user; // Người dùng tạo ra type này, null nếu là system defined

    @Column(name = "icon_url", length = 512)
    private String iconUrl;

    @Column(name = "is_system_defined")
    private Boolean isSystemDefined = false;

    @OneToMany(mappedBy = "walletType")
    private Set<Wallet> wallets;
}
