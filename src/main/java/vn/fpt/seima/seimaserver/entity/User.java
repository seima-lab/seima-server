package vn.fpt.seima.seimaserver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "user_full_name", length = 255)
    private String userFullName;

    @Column(name = "user_email", length = 255, nullable = false, unique = true)
    private String userEmail;

    @Column(name = "user_dob")
    private LocalDate userDob;


    @Column(name = "user_gender", nullable = false)
    private Boolean userGender;


    @Column(name = "user_phone_number", length = 25) // ERD là varchar(25)
    private String userPhoneNumber;


    @Column(name = "user_avatar_url", length = 512) // ERD là varchar(255), tăng lên 512 cho URL dài
    private String userAvatarUrl;

    @CreationTimestamp // Tự động gán thời gian tạo
    @Column(name = "user_created_date", updatable = false) // ERD là user_created_date (timestamp)
    private LocalDateTime userCreatedDate;

    @Column(name = "user_is_active") // ERD là user_is_active (bit)
    private Boolean userIsActive = true; // Mặc định là active

    // Các mối quan hệ
    @OneToMany(mappedBy = "user")
    private Set<Wallet> wallets;

    @OneToMany(mappedBy = "user")
    private Set<Transaction> transactions;

    @OneToMany(mappedBy = "user")
    private Set<Budget> budgets;

    @OneToMany(mappedBy = "user")
    private Set<Category> userDefinedCategories; // Danh mục do người dùng tự định nghĩa

    @OneToMany(mappedBy = "user") // user_id trong GroupMember
    private Set<GroupMember> groupMemberships;

    @OneToMany(mappedBy = "user")
    private Set<Notification> notifications;

    @OneToMany(mappedBy = "user") // user_id trong WalletType cho các type do user định nghĩa
    private Set<WalletType> userDefinedWalletTypes;
}
