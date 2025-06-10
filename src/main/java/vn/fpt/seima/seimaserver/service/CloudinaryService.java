package vn.fpt.seima.seimaserver.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface CloudinaryService {
    //truyen vao file vao
    Map uploadImage(MultipartFile file, String folderPath);
    //truyen vao publicId cua file
    boolean deleteImage(String publicId);
}
