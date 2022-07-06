package tech.sledger.investments.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PriceQuote {
    @JsonProperty("Ask")
    private BigDecimal ask;
    @JsonProperty("Bid")
    private BigDecimal bid;
    @JsonProperty("Mid")
    private BigDecimal mid;
}
