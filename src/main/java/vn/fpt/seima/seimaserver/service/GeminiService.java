package vn.fpt.seima.seimaserver.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.fpt.seima.seimaserver.dto.response.transaction.TransactionOcrResponse;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api-key}")
    private String apiKey;
    private final static String GEMINI_SERVER = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";
    private final RestTemplate restTemplate = new RestTemplate();

    public TransactionOcrResponse analyzeInvoiceFromOcrText(String ocrText, String imageUrl) {
        String url = GEMINI_SERVER + apiKey;

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

            output = output.trim();
            if (output.startsWith("```json")) {
                output = output.substring(7).trim(); // Bỏ '```json'
            }
            if (output.startsWith("```")) {
                output = output.substring(3).trim(); // Bỏ '```' nếu Gemini trả về ``` không kèm json
            }
            if (output.endsWith("```")) {
                output = output.substring(0, output.length() - 3).trim();
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule()); // để hỗ trợ parse LocalDateTime
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            TransactionOcrResponse result = mapper.readValue(output, TransactionOcrResponse.class);

            // Gán thêm imageUrl vào object
            result.setReceiptImageUrl(imageUrl);

            return result;
        } catch (Exception e) {
            throw new  IllegalArgumentException(e.getMessage()) ;
        }
    }

    private String buildPrompt(String ocrText) {
        return """
    Dưới đây là nội dung OCR từ một hóa đơn:

    %s

    Hãy trích xuất và trả về JSON với các trường sau:

    {
      "total_amount": "",          // Tổng tiền đã thanh toán
      "currency_code": "",         // Mã tiền tệ (ví dụ: VND, USD...)
      "transaction_date": "",      // Ngày giao dịch (định dạng ISO 8601 hoặc yyyy-MM-dd)
      "description_invoice": "",   // Đã thanh toán cho cái gì (tóm tắt, không cần liệt kê sản phẩm cụ thể)
      "customer_name": ""          // Họ tên người mua, nếu không rõ thì để null
    }

    Lưu ý:
    - Chỉ mô tả mục đích thanh toán (ví dụ: mua hàng, ăn uống, thanh toán dịch vụ, v.v).
    - Nếu không rõ họ tên đầy đủ của người mua, gán "customer_name": null.
    - Mẫu description_invoice sẽ là : "Thanh toán ..."
    - Số tiền mà có dấu chấm là số tiền lớn họ tách ra không phải số thập phân
    - total_amount là giá trị khách hàng phải trả chứ không phải tổng tiền
    """.formatted(ocrText);
    }

}
