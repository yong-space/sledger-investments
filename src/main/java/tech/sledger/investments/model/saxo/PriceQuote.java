package tech.sledger.investments.model.saxo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceQuote {
    @JsonProperty("Ask")
    private BigDecimal ask;
    @JsonProperty("Bid")
    private BigDecimal bid;
    @JsonProperty("Mid")
    private BigDecimal mid;
}
