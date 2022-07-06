package tech.sledger.investments.model;

import java.util.List;

public record SaxoInstrument(
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
