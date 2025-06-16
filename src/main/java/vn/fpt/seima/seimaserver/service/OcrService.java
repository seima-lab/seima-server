package vn.fpt.seima.seimaserver.service;

import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
     String extractTextFromFile(MultipartFile file) throws Exception;
}
