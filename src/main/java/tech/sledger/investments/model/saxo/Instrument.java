package tech.sledger.investments.model.saxo;

import java.util.List;

public record Instrument(
    String AssetType,
    String CurrencyCode,
    String Description,
    String ExchangeId,
    int GroupId,
    int Identifier,
    String IssuerCountry,
    int PrimaryListing,
    String SummaryType,
    String Symbol,
    List<String> TradableAs
) {}
