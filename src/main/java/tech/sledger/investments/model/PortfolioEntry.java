package tech.sledger.investments.model;

import java.math.BigDecimal;

public record PortfolioEntry(
    String symbol,
    String name,
    int position,
    BigDecimal price,
    BigDecimal dividends,
    BigDecimal profit,
    BigDecimal profitPercentage
) {}
