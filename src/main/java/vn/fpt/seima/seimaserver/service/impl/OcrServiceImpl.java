package vn.fpt.seima.seimaserver.service.impl;


import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.config.ocr.AzureFormRecognizerConfig;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOcrResponse;
import vn.fpt.seima.seimaserver.exception.GroupException;
import vn.fpt.seima.seimaserver.service.CloudinaryService;
import vn.fpt.seima.seimaserver.service.GeminiService;
import vn.fpt.seima.seimaserver.service.OcrService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Files.getFileExtension;

@Service
@AllArgsConstructor
public class OcrServiceImpl implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrServiceImpl.class);
    private final CloudinaryService cloudinaryService;
    private final GeminiService geminiService;
    private final AzureFormRecognizerConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpHeaders headers = new HttpHeaders();
    private final static String endPointUrl = "formrecognizer/documentModels/prebuilt-invoice:analyze?api-version=2023-07-31";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("jpg", "jpeg", "png");
    @Override
    public TransactionOcrResponse extractTextFromFile(MultipartFile file) throws Exception {
        try {
            validateImageFile(file);
            String analyzeUrl = config.getEndpoint() +endPointUrl;

            headers.set("Ocp-Apim-Subscription-Key", config.getApiKey());
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> request = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<Void> response = restTemplate.exchange(analyzeUrl, HttpMethod.POST, request, Void.class);

            Map uploadResult = cloudinaryService.uploadImage(
                    file, "transaction/receipt"
            );
            String imageUrl = ((String) uploadResult.get("secure_url"));

            String operationLocation = response.getHeaders().getFirst("Operation-Location");
            if (operationLocation == null) throw new IllegalStateException("Missing Operation-Location header");

            Thread.sleep(2000);

            HttpHeaders resultHeaders = new HttpHeaders();
            resultHeaders.set("Ocp-Apim-Subscription-Key", config.getApiKey());

            HttpEntity<Void> resultRequest = new HttpEntity<>(resultHeaders);
            ResponseEntity<String> resultResponse = restTemplate.exchange(
                    operationLocation,
                    HttpMethod.GET,
                    resultRequest,
                    String.class
            );
            TransactionOcrResponse ocrResponse =  geminiService.analyzeInvoiceFromOcrText(resultResponse.getBody(), imageUrl);
            log.info("reponse ocr:" + ocrResponse);
            if (ocrResponse.getAmount() == null || (ocrResponse.getAmount().compareTo(BigDecimal.ZERO) == 0 )) {
                throw new Exception("Scan Invoice Unsuccessful");
            }
            return ocrResponse;
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }


    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GroupException("Image file cannot be null or empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new GroupException("Image file size must be less than 5MB");
        }

        // Check file format
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new GroupException("Invalid image file");
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(fileExtension)) {
            throw new GroupException("Unsupported image format. Supported formats: " + String.join(", ", SUPPORTED_FORMATS));
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new GroupException("File must be an image");
        }
    }

}
