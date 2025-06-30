package vn.fpt.seima.seimaserver.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;


import java.util.Set;


@Data
@Entity
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Nullable, cho các category do người dùng định nghĩa
    private User user;


    @Column(name = "category_name", length = 255, nullable = false)
    private String categoryName;


    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", length = 50) // ERD là varchar(255), nên dùng ENUM
    private CategoryType categoryType; // INCOME, EXPENSE


    @Column(name = "category_icon_url", length = 512) // ERD là category_icon_url
    private String categoryIconUrl;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;


    @OneToMany(mappedBy = "parentCategory")
    private Set<Category> childCategories;


    @Column(name = "is_system_defined") // ERD là is_system_defined (bit)
    private Boolean isSystemDefined = false;


    @OneToMany(mappedBy = "category")
    private Set<Transaction> transactions;


    @OneToMany(mappedBy = "category")
    @JsonIgnore
    private Set<BudgetCategoryLimit> budgetCategoryLimits;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id") // Nullable, cho các category do người dùng định nghĩa cho group
    private Group group;
}
