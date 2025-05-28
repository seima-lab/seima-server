package vn.fpt.seima.seimaserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "app_group")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "group_name", length = 100, nullable = false)
    private String groupName;


    @CreationTimestamp
    @Column(name = "group_created_date", updatable = false) // ERD là group_created_date (timestamp)
    private LocalDateTime groupCreatedDate;

    @Column(name = "group_is_active") // ERD là group_is_active (bit)
    private Boolean groupIsActive = true;

    @Column(name = "group_avatar_url", length = 512 ,nullable = true)
    private String groupAvatarUrl;

    @OneToMany(mappedBy = "group")
    private Set<GroupMember> members;

    @OneToMany(mappedBy = "group")
    private Set<Transaction> transactions;
}
