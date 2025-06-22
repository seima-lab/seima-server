package vn.fpt.seima.seimaserver.service;

import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOcrResponse;

public interface OcrService {
     TransactionOcrResponse extractTextFromFile(MultipartFile file) throws Exception;
}
