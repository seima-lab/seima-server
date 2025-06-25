package vn.fpt.seima.seimaserver.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import vn.fpt.seima.seimaserver.service.impl.CloudinaryServiceImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    @BeforeEach
    void setUp() {
        // CloudinaryServiceImpl đã được inject ở trên
    }

    @Test
    void uploadImage_WhenSuccess_ReturnsUrl() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[1024]);
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("url", "http://cloudinary.com/test.png");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(expectedResponse);

        // When
        Map response = cloudinaryService.uploadImage(file, "avatar");

        // Then
        assertEquals(expectedResponse, response);
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    @Test
    void uploadImage_WhenFileSizeExceedsLimit_ThrowsException() {
        // Given
        byte[] largeFile = new byte[21 * 1024 * 1024]; // 21MB
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", largeFile);

        // Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            cloudinaryService.uploadImage(file, "avatar");
        });
        assertEquals("File size must be less than 20MB", exception.getMessage());
    }

    @Test
    void uploadImage_WhenIOException_ThrowsRuntimeException() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[1024]);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("IO Error"));

        // Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cloudinaryService.uploadImage(file, "avatar");
        });
        assertEquals("Upload image failed", exception.getMessage());
    }

    @Test
    void deleteImage_WhenSuccess_ReturnsTrue() throws Exception {
        // Given
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("result", "ok");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenReturn(responseMap);

        // When
        boolean result = cloudinaryService.deleteImage("test_public_id");

        // Then
        assertTrue(result);
    }

    @Test
    void deleteImage_WhenResultNotFound_ReturnsFalse() throws Exception {
        // Given
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("result", "not found");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenReturn(responseMap);

        // When
        boolean result = cloudinaryService.deleteImage("test_public_id");

        // Then
        assertFalse(result);
    }

    @Test
    void deleteImage_WhenIOException_ThrowsRuntimeException() throws Exception {
        // Given
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("IO Error"));

        // Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cloudinaryService.deleteImage("test_public_id");
        });
        assertEquals("Delete image failed", exception.getMessage());
    }
}
