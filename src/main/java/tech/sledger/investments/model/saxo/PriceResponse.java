package tech.sledger.investments.model.saxo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceResponse {
    @JsonProperty("Data")
    private List<PriceEntry> data;
}
