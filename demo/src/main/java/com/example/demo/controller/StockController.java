package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.StockPrediction;
import com.example.demo.service.StockPredictionService;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class StockController {

    private final StockPredictionService service;

    public StockController(StockPredictionService service) {
        this.service = service;
    }

    // ✅ FIX 1: return prediction object with company info
    @GetMapping("/predict/{stock}")
    public Map<String, Object> predict(@PathVariable String stock) {
        try {
            StockPrediction prediction = service.predict(stock);
            List<Map<String, Object>> prices = new java.util.ArrayList<>();
            java.time.LocalDate startDate;
            try {
                startDate = java.time.LocalDate.parse(prediction.getLatestDate());
            } catch (Exception parseEx) {
                startDate = java.time.LocalDate.now();
            }
            for (int i = 0; i < prediction.getPrices().size(); i++) {
                java.time.LocalDate futureDate = startDate.plusMonths(i + 1);
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("year", futureDate.getYear());
                item.put("month", futureDate.getMonthValue());
                item.put("price", prediction.getPrices().get(i));
                item.put("confidence", prediction.getConfidence());
                prices.add(item);
            }
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("currentPrice", prediction.getCurrentPrice());
            result.put("latestDate", prediction.getLatestDate());
            result.put("prices", prices);
            result.put("confidence", prediction.getConfidence());
            result.put("volatility", prediction.getVolatility());
            result.put("rsi", prediction.getRsi());
            result.put("macd", prediction.getMacd());
            result.put("marketSentiment", prediction.getMarketSentiment());
            result.put("dividendYield", prediction.getDividendYield());
            result.put("sharpeRatio", prediction.getSharpeRatio());
            result.put("isMock", prediction.isMock());
            result.put("history", prediction.getHistory());
            return result;
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", "Failed to fetch prediction: " + e.getMessage());
            error.put("details", e.toString());
            error.put("cause", e.getCause() != null ? e.getCause().toString() : "Unknown");
            return error;
        }
    }

    // ✅ FIX 2: correct method name
    @GetMapping("/stocks")
    public Set<String> stocks() {
        return service.getAllStocks();
    }
}
