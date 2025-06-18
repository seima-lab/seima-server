package vn.fpt.seima.seimaserver.service.impl;


import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import vn.fpt.seima.seimaserver.config.ocr.AzureFormRecognizerConfig;
import vn.fpt.seima.seimaserver.service.OcrService;

@Service
@AllArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final AzureFormRecognizerConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpHeaders headers = new HttpHeaders();
    private final static String endPointUrl = "/formrecognizer/documentModels/prebuilt-invoice:analyze?api-version=2024-11-30";
    @Override
    public String extractTextFromFile(MultipartFile file) throws Exception {

        String analyzeUrl = config.getEndpoint() +endPointUrl;

        headers.set("Ocp-Apim-Subscription-Key", config.getApiKey());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        HttpEntity<byte[]> request = new HttpEntity<>(file.getBytes(), headers);
        ResponseEntity<Void> response = restTemplate.exchange(analyzeUrl, HttpMethod.POST, request, Void.class);

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

        return resultResponse.getBody();
    }

}
