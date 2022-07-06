package tech.sledger.investments.model.saxo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.Instant;

@Data
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
