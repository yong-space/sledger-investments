package tech.sledger.investments.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PriceQuote {
    @JsonProperty("Ask")
    private float ask;
    @JsonProperty("Bid")
    private float bid;
    @JsonProperty("Mid")
    private float mid;
}
