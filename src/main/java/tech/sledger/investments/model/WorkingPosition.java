package tech.sledger.investments.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WorkingPosition {
    private BigDecimal latestPosition = BigDecimal.ZERO;
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalAmountLocal = BigDecimal.ZERO;
    private BigDecimal totalNotionalAmount = BigDecimal.ZERO;
    private BigDecimal dividends = BigDecimal.ZERO;
}
