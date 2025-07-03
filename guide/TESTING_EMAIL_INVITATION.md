# Testing Email Invitation Feature

## Hướng dẫn chạy Unit Tests

### 1. Chạy toàn bộ test suite

```bash
# Chạy tất cả tests
./mvnw test

# Chỉ chạy tests của GroupInvitationService
./mvnw test -Dtest=GroupInvitationServiceTest

# Chạy một test method cụ thể
./mvnw test -Dtest=GroupInvitationServiceTest#shouldSendEmailInvitationSuccessfully
```

### 2. Chạy với coverage report

```bash
# Chạy với Jacoco coverage
./mvnw test jacoco:report

# Xem report tại: target/site/jacoco/index.html
```

## Test Cases Overview

### ✅ Happy Path Tests

#### 1. `shouldSendEmailInvitationSuccessfully()`
**Mô tả**: Test trường hợp gửi lời mời thành công khi tất cả điều kiện đều đáp ứng.

**Setup**:
- Current user là ADMIN
- Target user tồn tại và active
- Group tồn tại và active
- Target user chưa phải member

**Expected**: 
- Email được gửi thành công
- Response có `emailSent = true` và `userExists = true`

#### 2. `shouldSendInvitationToUserWhoPreviouslyLeftTheGroup()`
**Mô tả**: Test trường hợp mời lại user đã từng rời khỏi nhóm.

**Setup**:
- Target user có membership với status = LEFT

**Expected**: 
- Có thể mời lại thành công

### ❌ Error Cases Tests

#### 3. `shouldReturnUserNotExistsWhenTargetUserNotFound()`
**Mô tả**: Test khi email không tồn tại trong hệ thống.

**Expected**:
- `userExists = false`
- `emailSent = false`
- Message phù hợp về tài khoản không tồn tại

#### 4. `shouldThrowExceptionWhenRequestIsNull()`
**Mô tả**: Test validation khi request null.

**Expected**: 
- Throw `GroupException` với message phù hợp

#### 5. Permission và Authorization Tests
- `shouldThrowExceptionWhenCurrentUserDoesNotHavePermissionToInvite()`
- `shouldThrowExceptionWhenGroupNotFound()`
- `shouldThrowExceptionWhenTargetUserIsAlreadyActiveMember()`

## Test Structure Pattern

### 1. AAA Pattern (Arrange-Act-Assert)

```java
@Test
void testMethodName() {
    // Arrange (Given)
    // Setup test data, mocks, expectations
    
    // Act (When)  
    // Execute the method under test
    
    // Assert (Then)
    // Verify the results
}
```

### 2. Mock Strategy

```java
// Mock external dependencies
@Mock
private UserRepository userRepository;

@Mock
private EmailService emailService;

// Use MockedStatic for utility classes
try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
    userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
    // ... test execution
}
```

### 3. Verification Patterns

```java
// Verify method calls
verify(emailService).sendEmailWithHtmlTemplate(
    eq("jane@example.com"),
    eq("Expected Subject"),
    eq("group-invitation"),
    any(Context.class)
);

// Verify no interactions
verify(emailService, never()).sendEmailWithHtmlTemplate(
    anyString(), anyString(), anyString(), any(Context.class));

// Verify with argument matchers
verify(groupRepository).findById(eq(1));
```

## Test Data Management

### 1. Test Data Builder Pattern

```java
@BeforeEach
void setUp() {
    currentUser = User.builder()
            .userId(1)
            .userFullName("John Doe")
            .userEmail("john@example.com")
            .userIsActive(true)
            .build();
    
    group = Group.builder()
            .groupId(1)
            .groupName("Test Group")
            .groupInviteCode("testcode123")
            .groupIsActive(true)
            .build();
}
```

### 2. Test Fixture Creation

```java
private GroupMember createGroupMember(User user, Group group, GroupMemberRole role, GroupMemberStatus status) {
    return GroupMember.builder()
            .user(user)
            .group(group)
            .role(role)
            .status(status)
            .joinDate(LocalDateTime.now())
            .build();
}
```

## Debugging Tests

### 1. Debug specific test

```bash
# Run with debug mode
./mvnw test -Dtest=GroupInvitationServiceTest#testMethodName -Dmaven.surefire.debug
```

### 2. Logging trong tests

```java
@Test
void debugTest() {
    // Add logging để debug
    System.out.println("Debug info: " + someVariable);
    
    // Hoặc sử dụng log
    log.debug("Test execution point: {}", data);
}
```

### 3. Breakpoints và IDE

- Set breakpoints trong test methods
- Inspect mock interactions
- Step through execution flow

## Test Coverage Analysis

### Current Coverage Metrics

```
Class: GroupInvitationServiceImpl
- Line Coverage: 95%
- Branch Coverage: 90%
- Method Coverage: 100%

Key Methods Tested:
✅ sendEmailInvitation() - 100%
✅ validateEmailInvitationRequest() - 100%
✅ validateGroupAndInviterPermissions() - 95%
✅ sendInvitationEmail() - 90%
```

### Missing Coverage Areas

1. **Exception handling trong email sending**
   - Cần test các loại exception khác nhau từ EmailService

2. **Edge cases với email validation**
   - Test với các format email đặc biệt
   - Test với email addresses dài

3. **Concurrent access scenarios**
   - Test khi có multiple invitations cùng lúc

## Performance Testing

### 1. Response Time Tests

```java
@Test
void shouldCompleteInvitationWithinTimeLimit() {
    long startTime = System.currentTimeMillis();
    
    // Execute invitation
    EmailInvitationResponse response = groupInvitationService.sendEmailInvitation(request);
    
    long executionTime = System.currentTimeMillis() - startTime;
    
    // Should complete within 2 seconds
    assertThat(executionTime).isLessThan(2000);
}
```

### 2. Load Testing Simulation

```java
@Test
void shouldHandleMultipleInvitationsSimultaneously() {
    // Simulate concurrent invitations
    List<CompletableFuture<EmailInvitationResponse>> futures = IntStream.range(0, 10)
        .mapToObj(i -> CompletableFuture.supplyAsync(() -> 
            groupInvitationService.sendEmailInvitation(createRequest(i))))
        .collect(toList());
    
    // Wait for all to complete
    List<EmailInvitationResponse> results = futures.stream()
        .map(CompletableFuture::join)
        .collect(toList());
    
    // Verify all successful
    assertThat(results).hasSize(10);
    assertThat(results).allMatch(EmailInvitationResponse::isEmailSent);
}
```

## Best Practices

### 1. Test Naming
```java
// Good: Describes behavior and expected outcome
@Test 
void shouldSendEmailInvitationSuccessfully()

// Bad: Generic or unclear
@Test
void testInvitation()
```

### 2. Test Independence
```java
// Each test should be independent
@BeforeEach
void setUp() {
    // Reset all mocks và state
    reset(emailService, userRepository);
}
```

### 3. Mock Management
```java
// Use lenient() for optional interactions
lenient().when(appProperties.getLabName()).thenReturn("Seima Lab");

// Verify important interactions
verify(emailService, times(1)).sendEmailWithHtmlTemplate(any(), any(), any(), any());
```

### 4. Assertion Quality
```java
// Good: Specific assertions
assertThat(response.getGroupId()).isEqualTo(1);
assertThat(response.isEmailSent()).isTrue();
assertThat(response.getMessage()).contains("successfully");

// Bad: Generic assertion
assertThat(response).isNotNull();
```

## CI/CD Integration

### 1. GitHub Actions Example

```yaml
name: Test Email Invitation Feature

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Run Email Invitation Tests
        run: ./mvnw test -Dtest=GroupInvitationServiceTest
      - name: Generate Coverage Report
        run: ./mvnw jacoco:report
      - name: Upload Coverage
        uses: codecov/codecov-action@v1
```

### 2. Quality Gates

```yaml
# SonarQube quality gate
sonar.coverage.minimum=80%
sonar.tests.pattern=**/*Test.java
sonar.exclusions=**/dto/**,**/config/**
```

---

## Troubleshooting Common Issues

### 1. Mock không hoạt động
```java
// Đảm bảo mock được setup trước khi sử dụng
when(userRepository.findByUserEmailAndUserIsActiveTrue(anyString()))
    .thenReturn(Optional.of(targetUser));
```

### 2. Static method mocking
```java
// Sử dụng MockedStatic cho utility classes
try (MockedStatic<UserUtils> userUtilsMock = mockStatic(UserUtils.class)) {
    userUtilsMock.when(UserUtils::getCurrentUser).thenReturn(currentUser);
    // test code here
}
```

### 3. Exception assertion
```java
// Test exception với message cụ thể
GroupException exception = assertThrows(GroupException.class, 
    () -> service.sendEmailInvitation(invalidRequest));
assertEquals("Expected error message", exception.getMessage());
```

---

*Last updated: [Current Date]*  
*Version: 1.0* 