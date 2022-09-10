package tech.sledger.investments.model.saxo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceEntry {
    @JsonProperty("LastUpdated")
    private Instant lastUpdated;
    @JsonProperty("Uic")
    private int identifier;
    @JsonProperty("Quote")
    private PriceQuote quote;
    @JsonProperty("PriceInfo")
    private PriceInfo priceInfo;
    @JsonProperty("PriceInfoDetails")
    private PriceInfoDetails priceInfoDetails;
}
