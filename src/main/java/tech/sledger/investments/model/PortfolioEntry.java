package tech.sledger.investments.model;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PortfolioEntry {
    private String symbol;
    private String name;
    private BigDecimal position;
    private BigDecimal amount;
    private BigDecimal price;
    private BigDecimal dividends;
    private BigDecimal changeToday;
    private BigDecimal changeTodayPercentage;
    private BigDecimal profit;
    private BigDecimal profitPercentage;
}
