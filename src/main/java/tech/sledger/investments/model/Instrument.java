package tech.sledger.investments.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import java.math.BigDecimal;

@Data
@Builder
public class Instrument {
    @Id
    private int id;
    private String name;
    private String symbol;
    private String currency;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;
}
