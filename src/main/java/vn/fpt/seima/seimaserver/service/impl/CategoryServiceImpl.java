package vn.fpt.seima.seimaserver.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fpt.seima.seimaserver.dto.request.category.CreateCategoryRequest;
import vn.fpt.seima.seimaserver.dto.response.category.CategoryResponse;
import vn.fpt.seima.seimaserver.entity.*;
import vn.fpt.seima.seimaserver.exception.ResourceNotFoundException;
import vn.fpt.seima.seimaserver.mapper.CategoryMapper;
import vn.fpt.seima.seimaserver.repository.*;
import vn.fpt.seima.seimaserver.service.BudgetService;
import vn.fpt.seima.seimaserver.service.CategoryService;
import vn.fpt.seima.seimaserver.service.RedisService;
import vn.fpt.seima.seimaserver.service.WalletService;
import vn.fpt.seima.seimaserver.util.UserUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private CategoryRepository categoryRepository;
    private BudgetCategoryLimitRepository budgetCategoryLimitRepository;
    private TransactionRepository transactionRepository;
    private BudgetService budgetService;
    private WalletService walletService;
    private RedisService redisService;

    @Override
    public List<CategoryResponse> getAllCategoryByTypeAndUser(Integer categoryType, Integer groupId) {
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException("User not found ");
        }
        CategoryType type = CategoryType.fromCode(categoryType);

        List<Category> categories;

        if (groupId != null && groupId != 0){
            categories = categoryRepository.findByCategoryTypeAndGroup_GroupIdOrGroupIsNull(type, groupId);
        }else{
            categories = categoryRepository.findByCategoryTypeAndUser_UserIdOrUserIsNull(type, user.getUserId());
        }
        return categories.stream().map(categoryMapper::toResponse).toList();
    }

    @Override
    public CategoryResponse getCategoryById(int id) {
        Category category = categoryRepository.findById(id).orElseThrow(
                ()->new ResourceNotFoundException("Category not found with id " + id)
        );
        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse saveCategory(CreateCategoryRequest request) {
        User user = UserUtils.getCurrentUser();

        Group group = null;
        boolean isDuplicate = false;

        if (user == null) {
            throw new IllegalArgumentException("Cannot identify current user.");
        }

        if (request == null) {
            throw new ResourceNotFoundException("Category request is null");
        }

        if (request.getCategoryName() == null || request.getCategoryType() == null) {
            throw new IllegalArgumentException("Category name and type must not be null.");
        }

        if(request.getGroupId()!= null){
            group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + request.getGroupId()));

            if (!groupMemberRepository.existsByUserUserIdAndGroupGroupId(user.getUserId(), group.getGroupId())) {
                throw new IllegalArgumentException("You are not authorized to create this group category.");
            }

            isDuplicate = categoryRepository.existsByCategoryNameAndTypeAndGroup_GroupId(
                    request.getCategoryName().trim(), request.getCategoryType(), request.getGroupId()
            );
        } else {
            isDuplicate = categoryRepository.existsByCategoryNameAndTypeAndUser_UserId(
                    request.getCategoryName().trim(), request.getCategoryType(), user.getUserId()
            );
        }

        if (isDuplicate) {
            throw new IllegalArgumentException("A category with the same name already exists in the same scope and type.");
        }

        Category category = categoryMapper.toEntity(request);
        if (group != null) {
            category.setGroup(group);
            category.setUser(null);
        } else {
            category.setUser(user);
            category.setGroup(null);
        }
        category.setUser(user);
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public CategoryResponse updateCategory(Integer id, CreateCategoryRequest request) {
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalArgumentException("Unable to identify the current user.");
        }

        if (request == null || request.getCategoryName() == null || request.getCategoryType() == null) {
            throw new IllegalArgumentException("Category name and type must not be null.");
        }

        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));

        if (Boolean.TRUE.equals(existingCategory.getIsSystemDefined())) {
            throw new IllegalArgumentException("System-defined categories cannot be updated.");
        }

        if (categoryRepository.existsByCategoryName(request.getCategoryName().trim()) &&
                !existingCategory.getCategoryName().equals(request.getCategoryName().trim())) {
            throw new IllegalArgumentException("Category name already exists");
        }
        Group group = existingCategory.getGroup();
        boolean isDuplicate;

        // Permission check: only the owner or group member can update the category
        if (group != null) {
            if(!groupMemberRepository.existsByGroupAndUserAndRole(group.getGroupId(), user.getUserId(), GroupMemberRole.ADMIN) ) {
                throw new IllegalArgumentException("You are not authorized to update this group category.");
            };
        } else if (!user.getUserId().equals(existingCategory.getUser().getUserId())) {
            throw new IllegalArgumentException("You are not authorized to update this personal category.");
        }

        // Duplication check: based on existing ownership (user or group)
        if (group != null) {
            isDuplicate = categoryRepository.existsByCategoryNameAndTypeAndGroup_GroupId(
                    request.getCategoryName().trim(), request.getCategoryType(), group.getGroupId()
            );
        } else {
            isDuplicate = categoryRepository.existsByCategoryNameAndTypeAndUser_UserId(
                    request.getCategoryName().trim(), request.getCategoryType(), user.getUserId()
            );
        }

        if (isDuplicate && !existingCategory.getCategoryName().trim().equals(request.getCategoryName().trim())) {
            throw new IllegalArgumentException("A category with the same name already exists in the same scope and type.");
        }

        categoryMapper.updateCategoryFromDto(request, existingCategory);

        Category savedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toResponse(savedCategory);
    }


    @Override
    @Transactional
    public void deleteCategory(int id) {
        Group group = null;

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id " + id));

        // Không cho xóa nếu là hệ thống
        if (Boolean.TRUE.equals(category.getIsSystemDefined())) {
            throw new IllegalArgumentException("System-defined categories cannot be deleted.");
        }

        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalArgumentException("Cannot identify current user.");
        }

        if (category.getUser() == null) {
            if(!groupMemberRepository.existsByGroupAndUserAndRole(category.getGroup().getGroupId(), currentUser.getUserId(), GroupMemberRole.ADMIN) ) {
                throw new IllegalArgumentException("You are not authorized to delete this category.");
            };
        }

        if(!currentUser.getUserId().equals(category.getUser().getUserId())) {
            throw new IllegalArgumentException("You are not authorized to delete this category.");
        }
        List<Transaction> transactions = transactionRepository.findAllByCategory_CategoryId(id);
        for (Transaction transaction : transactions) {
            budgetService.reduceAmount(currentUser.getUserId(),
                    id,
                    transaction.getAmount(),
                    transaction.getTransactionDate(),
                    "update-add",
                    transaction.getCurrencyCode(), transaction.getWallet().getId(), BigDecimal.ZERO);
            walletService.reduceAmount(transaction.getWallet().getId(),
                    transaction.getAmount(),
                    "update-add",
                    transaction.getCurrencyCode());
        }
        String financialHealthKey = "financial_health:" + currentUser.getUserId();
        redisService.delete(financialHealthKey);
        transactionRepository.deleteByCategory_CategoryId(id);
        budgetCategoryLimitRepository.deleteByCategory_CategoryId(id);
        categoryRepository.deleteById(id);
    }
}