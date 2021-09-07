package tech.sledger.investments.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.Instant;

@Data
public class PriceEntry {
    @JsonProperty("LastUpdated")
    private Instant lastUpdated;
    @JsonProperty("Quote")
    private PriceQuote quote;
    @JsonProperty("Uic")
    private int identifier;
}
