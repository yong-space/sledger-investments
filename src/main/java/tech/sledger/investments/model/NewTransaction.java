package tech.sledger.investments.model;

import java.math.BigDecimal;
import java.time.Instant;

public record NewTransaction(
    TransactionType type,
    Instant date,
    String instrument,
    String ticker,
    BigDecimal price,
    BigDecimal quantity,
    BigDecimal notional
) {}
