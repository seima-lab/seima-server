package vn.fpt.seima.seimaserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.fpt.seima.seimaserver.dto.request.group.CreateGroupRequest;
import vn.fpt.seima.seimaserver.dto.response.group.GroupResponse;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.service.GroupService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupController.class)
@SuppressWarnings("deprecation") // Suppress MockBean deprecation warning
class GroupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GroupService groupService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateGroupRequest validRequest;
    private GroupResponse mockResponse;

    @BeforeEach
    void setUp() {
        validRequest = new CreateGroupRequest();
        validRequest.setGroupName("Test Group");
        validRequest.setGroupAvatarUrl("http://example.com/avatar.jpg");

        mockResponse = new GroupResponse();
        mockResponse.setGroupId(1);
        mockResponse.setGroupName("Test Group");
        mockResponse.setGroupIsActive(true);
        mockResponse.setGroupCreatedDate(LocalDateTime.now());
    }

    @Test
    void createGroup_Success() throws Exception {
        // Given
        when(groupService.createGroup(any(CreateGroupRequest.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/groups/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Group created successfully"))
                .andExpect(jsonPath("$.data.groupId").value(1))
                .andExpect(jsonPath("$.data.groupName").value("Test Group"))
                .andExpect(jsonPath("$.data.groupIsActive").value(true));
    }

    @Test
    void createGroup_EmptyGroupName_ReturnsBadRequest() throws Exception {
        // Given
        validRequest.setGroupName("");

        // When & Then
        mockMvc.perform(post("/api/v1/groups/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void createGroup_NullGroupName_ReturnsBadRequest() throws Exception {
        // Given
        validRequest.setGroupName(null);

        // When & Then
        mockMvc.perform(post("/api/v1/groups/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void createGroup_GroupNameTooLong_ReturnsBadRequest() throws Exception {
        // Given
        String longName = "a".repeat(101); // Exceeds 100 character limit
        validRequest.setGroupName(longName);

        // When & Then
        mockMvc.perform(post("/api/v1/groups/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void createGroup_ServiceThrowsGroupException_ReturnsBadRequest() throws Exception {
        // Given
        when(groupService.createGroup(any(CreateGroupRequest.class)))
                .thenThrow(new GroupException("Unable to identify the current user"));

        // When & Then
        mockMvc.perform(post("/api/v1/groups/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Unable to identify the current user"));
    }

    @Test
    void createGroup_InvalidJson_ReturnsBadRequest() throws Exception {
        // Given
        String invalidJson = "{\"groupName\": }"; // Invalid JSON

        // When & Then
        mockMvc.perform(post("/api/v1/groups/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGroup_MissingContentType_ReturnsUnsupportedMediaType() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/groups/create")
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void createGroup_AvatarUrlTooLong_ReturnsBadRequest() throws Exception {
        // Given
        String longUrl = "http://example.com/" + "a".repeat(500); // Exceeds 512 character limit
        validRequest.setGroupAvatarUrl(longUrl);

        // When & Then
        mockMvc.perform(post("/api/v1/groups/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void createGroup_ValidAvatarUrl_Success() throws Exception {
        // Given
        validRequest.setGroupAvatarUrl("https://example.com/valid-avatar.jpg");
        when(groupService.createGroup(any(CreateGroupRequest.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/groups/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Group created successfully"));
    }

    @Test
    void createGroup_EdgeCaseGroupName100Characters_Success() throws Exception {
        // Given
        String exactLength = "a".repeat(100); // Exactly 100 characters (should pass)
        validRequest.setGroupName(exactLength);
        when(groupService.createGroup(any(CreateGroupRequest.class))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/groups/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(201));
    }
} 