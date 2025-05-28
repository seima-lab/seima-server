package vn.fpt.seima.seimaserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "budget")
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_id")
    private Integer budgetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "budget_name", length = 255, nullable = false) // ERD là varchar(255)
    private String budgetName;

    @Column(name = "start_date") // ERD là timestamp
    private LocalDateTime startDate;

    @Column(name = "end_date") // ERD là timestamp
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", length = 50) // ERD là varchar(255), nên dùng ENUM
    private PeriodType periodType; // WEEKLY, MONTHLY, YEARLY, CUSTOM

    @Column(name = "overall_amount_limit", precision = 18, scale = 2)
    private BigDecimal overallAmountLimit;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BudgetCategoryLimit> budgetCategoryLimits;
}
