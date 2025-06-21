package vn.fpt.seima.seimaserver.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.service.OcrService;

@RestController
@RequestMapping("/api/ocr")
@AllArgsConstructor
public class OcrController {

    private OcrService ocrService;

//    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<String> scanInvoice(@RequestParam("file") MultipartFile file) {
//        try {
//
//
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("Lỗi xử lý OCR: " + e.getMessage());
//        }
//    }
}
