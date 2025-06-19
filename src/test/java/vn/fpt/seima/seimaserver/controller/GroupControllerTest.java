package vn.fpt.seima.seimaserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.fpt.seima.seimaserver.dto.response.group.GroupDetailResponse;
import vn.fpt.seima.seimaserver.dto.response.group.GroupMemberResponse;
import vn.fpt.seima.seimaserver.entity.GroupMemberRole;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.service.GroupService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GroupService groupService;

    @Autowired
    private ObjectMapper objectMapper;

    private GroupDetailResponse testGroupDetailResponse;

    @BeforeEach
    void setUp() {
        // Setup leader
        GroupMemberResponse leader = new GroupMemberResponse();
        leader.setUserId(1);
        leader.setUserFullName("John Doe");
        leader.setUserAvatarUrl("https://example.com/john-avatar.jpg");
        leader.setRole(GroupMemberRole.ADMIN);

        // Setup members
        GroupMemberResponse member1 = new GroupMemberResponse();
        member1.setUserId(2);
        member1.setUserFullName("Jane Smith");
        member1.setUserAvatarUrl("https://example.com/jane-avatar.jpg");
        member1.setRole(GroupMemberRole.MEMBER);

        GroupMemberResponse member2 = new GroupMemberResponse();
        member2.setUserId(3);
        member2.setUserFullName("Bob Johnson");
        member2.setUserAvatarUrl("https://example.com/bob-avatar.jpg");
        member2.setRole(GroupMemberRole.MEMBER);

        List<GroupMemberResponse> members = Arrays.asList(member1, member2);

        // Setup group detail response
        testGroupDetailResponse = new GroupDetailResponse();
        testGroupDetailResponse.setGroupId(1);
        testGroupDetailResponse.setGroupName("Test Group");
        testGroupDetailResponse.setGroupInviteCode("test-invite-code-123");
        testGroupDetailResponse.setGroupAvatarUrl("https://example.com/group-avatar.jpg");
        testGroupDetailResponse.setGroupCreatedDate(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        testGroupDetailResponse.setGroupIsActive(true);
        testGroupDetailResponse.setGroupLeader(leader);
        testGroupDetailResponse.setMembers(members);
        testGroupDetailResponse.setTotalMembersCount(3);
    }

    @Test
    void getGroupDetail_ShouldReturnGroupDetailResponse_WhenValidGroupId() throws Exception {
        // Given
        Integer groupId = 1;
        when(groupService.getGroupDetail(groupId)).thenReturn(testGroupDetailResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}", groupId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Group detail retrieved successfully"))
                .andExpect(jsonPath("$.data.groupId").value(1))
                .andExpect(jsonPath("$.data.groupName").value("Test Group"))
                .andExpect(jsonPath("$.data.groupInviteCode").value("test-invite-code-123"))
                .andExpect(jsonPath("$.data.groupAvatarUrl").value("https://example.com/group-avatar.jpg"))
                .andExpect(jsonPath("$.data.groupCreatedDate").value("2024-01-01 10:00:00"))
                .andExpect(jsonPath("$.data.groupIsActive").value(true))
                .andExpect(jsonPath("$.data.totalMembersCount").value(3))
                .andExpect(jsonPath("$.data.groupLeader.userId").value(1))
                .andExpect(jsonPath("$.data.groupLeader.userFullName").value("John Doe"))
                .andExpect(jsonPath("$.data.groupLeader.userAvatarUrl").value("https://example.com/john-avatar.jpg"))
                .andExpect(jsonPath("$.data.groupLeader.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.members").isArray())
                .andExpect(jsonPath("$.data.members.length()").value(2))
                .andExpect(jsonPath("$.data.members[0].userId").value(2))
                .andExpect(jsonPath("$.data.members[0].userFullName").value("Jane Smith"))
                .andExpect(jsonPath("$.data.members[0].role").value("MEMBER"))
                .andExpect(jsonPath("$.data.members[1].userId").value(3))
                .andExpect(jsonPath("$.data.members[1].userFullName").value("Bob Johnson"))
                .andExpect(jsonPath("$.data.members[1].role").value("MEMBER"));
    }

    @Test
    void getGroupDetail_ShouldReturnNotFound_WhenGroupNotExists() throws Exception {
        // Given
        Integer groupId = 999;
        when(groupService.getGroupDetail(groupId))
                .thenThrow(new GroupException("Group not found with ID: " + groupId));

        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}", groupId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGroupDetail_ShouldReturnBadRequest_WhenInvalidGroupId() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGroupDetail_ShouldReturnBadRequest_WhenGroupIsInactive() throws Exception {
        // Given
        Integer groupId = 1;
        when(groupService.getGroupDetail(groupId))
                .thenThrow(new GroupException("Group is not active"));

        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}", groupId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGroupDetail_ShouldHandleNullAvatarUrls() throws Exception {
        // Given
        Integer groupId = 1;
        testGroupDetailResponse.setGroupAvatarUrl(null);
        testGroupDetailResponse.getGroupLeader().setUserAvatarUrl(null);
        testGroupDetailResponse.getMembers().get(0).setUserAvatarUrl(null);

        when(groupService.getGroupDetail(groupId)).thenReturn(testGroupDetailResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/groups/{groupId}", groupId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupAvatarUrl").doesNotExist())
                .andExpect(jsonPath("$.data.groupLeader.userAvatarUrl").doesNotExist())
                .andExpect(jsonPath("$.data.members[0].userAvatarUrl").doesNotExist());
    }
} 