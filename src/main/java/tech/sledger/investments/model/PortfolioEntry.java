package tech.sledger.investments.model;

public record PortfolioEntry(
    String name,
    float price,
    int position,
    float profit
) {}
