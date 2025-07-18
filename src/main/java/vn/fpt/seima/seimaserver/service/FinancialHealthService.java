package vn.fpt.seima.seimaserver.service;

import vn.fpt.seima.seimaserver.dto.response.budget.FinancialHealthResponse;

public interface FinancialHealthService {
    FinancialHealthResponse calculateScore();
}
