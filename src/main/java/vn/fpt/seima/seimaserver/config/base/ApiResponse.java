package vn.fpt.seima.seimaserver.config.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;
    private String errorCode;

    public ApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        // errorCode sẽ là null (mặc định)
    }

    public ApiResponse(int status, String message) {
        this.status = status;
        this.message = message;
        // data và errorCode sẽ là null (mặc định)
    }
}
