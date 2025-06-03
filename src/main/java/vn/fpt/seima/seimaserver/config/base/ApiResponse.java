package vn.fpt.seima.seimaserver.config.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ApiResponse<T> {
    private int statusCode;
    private String message;
    private T data; // Tùy chọn, nếu bạn muốn trả về thêm dữ liệu

}
