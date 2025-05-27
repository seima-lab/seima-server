package vn.fpt.seima.seimaserver.config.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ApiResponse {
    private int status;
    private String message;
    private T date;
    private String errorCode;
}
