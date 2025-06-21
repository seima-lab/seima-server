package vn.fpt.seima.seimaserver.service;



import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mock.web.MockMultipartFile;
import vn.fpt.seima.seimaserver.service.CloudinaryService;
import vn.fpt.seima.seimaserver.service.impl.CloudinaryServiceImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cloudinaryService = new CloudinaryServiceImpl(cloudinary);
    }

    @Test
    void testUploadImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                new byte[1024]
        );

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("url", "http://cloudinary.com/test.png");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(expectedResponse);

        Map response = cloudinaryService.uploadImage(file, "avatar");

        assertEquals(expectedResponse, response);
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    @Test
    void testUploadImage_FileSizeExceedsLimit() {
        byte[] largeFile = new byte[21 * 1024 * 1024]; // >20MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                largeFile
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            cloudinaryService.uploadImage(file, "avatar");
        });

        assertEquals("File size must be less than 20MB", exception.getMessage());
    }

    @Test
    void testUploadImage_IOException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                new byte[1024]
        );

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("IO Error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cloudinaryService.uploadImage(file, "avatar");
        });

        assertEquals("Upload image failed", exception.getMessage());
    }

    @Test
    void testDeleteImage_Success() throws Exception {
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("result", "ok");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenReturn(responseMap);

        boolean result = cloudinaryService.deleteImage("test_public_id");
        assertTrue(result);
    }

    @Test
    void testDeleteImage_Failed() throws Exception {
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("result", "not found");

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenReturn(responseMap);

        boolean result = cloudinaryService.deleteImage("test_public_id");
        assertFalse(result);
    }

    @Test
    void testDeleteImage_IOException() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("IO Error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cloudinaryService.deleteImage("test_public_id");
        });

        assertEquals("Delete image failed", exception.getMessage());
    }
}
