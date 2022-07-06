package tech.sledger.investments.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class Transaction {
    @Id
    private int id;
    private Instant date;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal price;
    private String ticker;
    private Integer quantity;
    private Integer instrumentId;
    private BigDecimal fxRate;
}
