package tech.sledger.investments.model.saxo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceInfoDetails {
    @JsonProperty("LastTraded")
    private BigDecimal lastTraded;
}
