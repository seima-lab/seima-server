package vn.fpt.seima.seimaserver.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.service.CloudinaryService;

import java.io.IOException;
import java.util.Map;

@Service
@AllArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public Map uploadImage(MultipartFile file, String subFolder) {
        try {
            String fullFolderPath = "seima-server/" + (subFolder != null ? subFolder.trim() : "");

            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", fullFolderPath,
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false
            );

            return cloudinary.uploader().upload(file.getBytes(), options);
        } catch (IOException e) {
            throw new RuntimeException("Upload image failed", e);
        }
    }

    @Override
    public boolean deleteImage(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (IOException e) {
            throw new RuntimeException("Delete image failed", e);
        }
    }
}