package tech.sledger.investments.model;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class Instrument {
    private String name;
    private String symbol;
    private String currency;
    private int identifier;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;
}
