package vn.fpt.seima.seimaserver.service.impl;


import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.config.ocr.AzureFormRecognizerConfig;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOcrResponse;
import vn.fpt.seima.seimaserver.service.CloudinaryService;
import vn.fpt.seima.seimaserver.service.GeminiService;
import vn.fpt.seima.seimaserver.service.OcrService;

import java.util.Map;

@Service
@AllArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final CloudinaryService cloudinaryService;
    private final GeminiService geminiService;
    private final AzureFormRecognizerConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpHeaders headers = new HttpHeaders();
    private final static String endPointUrl = "formrecognizer/documentModels/prebuilt-invoice:analyze?api-version=2023-07-31";
    @Override
    public TransactionOcrResponse extractTextFromFile(MultipartFile file) throws Exception {

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

        Thread.sleep(3000);

        HttpHeaders resultHeaders = new HttpHeaders();
        resultHeaders.set("Ocp-Apim-Subscription-Key", config.getApiKey());

        HttpEntity<Void> resultRequest = new HttpEntity<>(resultHeaders);
        ResponseEntity<String> resultResponse = restTemplate.exchange(
                operationLocation,
                HttpMethod.GET,
                resultRequest,
                String.class
        );
      return  geminiService.analyzeInvoiceFromOcrText(resultResponse.getBody(), imageUrl);

    }

}
