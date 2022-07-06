package tech.sledger.investments.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Instrument {
    private String name;
    private String symbol;
    private String currency;
    private int identifier;
    private float price;
}
