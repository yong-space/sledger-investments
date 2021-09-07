package tech.sledger.investments.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class PriceResponse {
    @JsonProperty("Data")
    private List<PriceEntry> data;
}
