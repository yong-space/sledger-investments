package tech.sledger.investments.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import java.math.BigDecimal;

@Data
public class Position {
    @Id
    private int id;
    private String symbol;
    private int position;
    private BigDecimal buyPrice;
    private BigDecimal buyFees;
    private BigDecimal buyFx;
    private BigDecimal dividends;
}
