package com.example.demo.model;

import java.util.List;

public class StockPrediction {

    private String stock;          // Stock symbol (AAPL, MSFT, etc.)
    private double currentPrice;   // Current month-end closing price
    private String latestDate;     // Latest month-end date
    private List<Double> prices;   // Predicted future prices
    private double volatility;     // Predicted volatility over next 5 years
    private double confidence;     // Prediction confidence percentage
    private double rsi;           // Relative Strength Index (technical indicator)
    private double macd;          // Moving Average Convergence Divergence
    private String marketSentiment; // Bullish/Bearish/Neutral
    private double dividendYield; // Estimated dividend yield
    private double sharpeRatio;   // Risk-adjusted return metric
    private boolean isMock;       // Flag indicating if data is simulated
    private List<Double> history; // Historical prices (past 120 months)

    public StockPrediction(
            String stock,
            double currentPrice,
            String latestDate,
            List<Double> prices,
            double volatility,
            double confidence,
            double rsi,
            double macd,
            String marketSentiment,
            double dividendYield,
            double sharpeRatio,
            boolean isMock,
            List<Double> history
    ) {
        this.stock = stock;
        this.currentPrice = currentPrice;
        this.latestDate = latestDate;
        this.prices = prices;
        this.volatility = volatility;
        this.confidence = confidence;
        this.rsi = rsi;
        this.macd = macd;
        this.marketSentiment = marketSentiment;
        this.dividendYield = dividendYield;
        this.sharpeRatio = sharpeRatio;
        this.isMock = isMock;
        this.history = history;
    }

    public String getStock() {
        return stock;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public List<Double> getPrices() {
        return prices;
    }

    public double getVolatility() {
        return volatility;
    }

    public double getConfidence() {
        return confidence;
    }

    public double getRsi() {
        return rsi;
    }

    public double getMacd() {
        return macd;
    }

    public String getMarketSentiment() {
        return marketSentiment;
    }

    public double getDividendYield() {
        return dividendYield;
    }

    public double getSharpeRatio() {
        return sharpeRatio;
    }

    public String getLatestDate() {
        return latestDate;
    }

    public boolean isMock() {
        return isMock;
    }

    public List<Double> getHistory() {
        return history;
    }
}
