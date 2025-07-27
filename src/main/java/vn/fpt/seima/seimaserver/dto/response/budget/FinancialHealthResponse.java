package vn.fpt.seima.seimaserver.dto.response.budget;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinancialHealthResponse {
    private Integer score;
    private String level;
}
