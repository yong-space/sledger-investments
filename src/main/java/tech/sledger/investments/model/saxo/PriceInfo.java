package tech.sledger.investments.model.saxo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceInfo {
    @JsonProperty("High")
    private BigDecimal high;
    @JsonProperty("Low")
    private BigDecimal low;
    @JsonProperty("NetChange")
    private BigDecimal netChange;
    @JsonProperty("PercentChange")
    private BigDecimal percentChange;
}
