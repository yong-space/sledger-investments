package tech.sledger.investments.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.math.BigDecimal;

@Data
@Builder
public class Position {
    @Id
    private int id;
    @DBRef
    private Instrument instrument;
    private BigDecimal position;
    private BigDecimal buyPrice;
    private BigDecimal buyFees;
    private BigDecimal buyFx;
    private BigDecimal dividends;
}
