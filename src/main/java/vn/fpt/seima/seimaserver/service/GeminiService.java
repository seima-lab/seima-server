package vn.fpt.seima.seimaserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String analyzeInvoiceFromOcrText(String ocrText) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

        String prompt = buildPrompt(ocrText);

        // Payload gửi tới Gemini
        String jsonBody = """
            {
              "contents": [{
                "parts": [{
                  "text": "%s"
                }]
              }]
            }
            """.formatted(prompt.replace("\"", "\\\""));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            Map candidate = (Map) ((Map) ((java.util.List) response.getBody().get("candidates")).get(0)).get("content");
            String output = ((Map) ((java.util.List) candidate.get("parts")).get(0)).get("text").toString();

            return output;
        } catch (Exception e) {
            return "❌ Lỗi khi gọi Gemini API: " + e.getMessage();
        }
    }

    private String buildPrompt(String ocrText) {
        return """
        Dưới đây là nội dung OCR của một hóa đơn:

        %s

        Hãy trích xuất thông tin sau thành định dạng JSON:
        {
          "total_amount": "",
          "currency_code": "",
          "transaction_date": "",
          "description_invoice": "",
          "customer_name": ""
        }
        """.formatted(ocrText);
    }
}
