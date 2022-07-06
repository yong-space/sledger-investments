package tech.sledger.investments.model;

import org.springframework.data.annotation.Id;
import java.math.BigDecimal;

public record Position(
    @Id int id,
    int position,
    BigDecimal buyPrice,
    BigDecimal buyFees,
    BigDecimal buyFx,
    BigDecimal dividends
) {}
