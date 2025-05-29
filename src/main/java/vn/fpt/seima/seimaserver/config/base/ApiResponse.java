package vn.fpt.seima.seimaserver.config.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data; // Tùy chọn, nếu bạn muốn trả về thêm dữ liệu

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
