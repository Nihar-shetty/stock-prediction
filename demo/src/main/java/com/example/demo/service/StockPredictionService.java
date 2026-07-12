package com.example.demo.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.example.demo.model.StockPrediction;

@Service
public class StockPredictionService {

    private static final String API_KEY = "UV7984JMDBYYLQLU";

    private static final List<String> STOCKS = List.of(
            "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA",
            "META", "NVDA", "NFLX", "IBM", "ORCL",
            "AMD", "INTC", "PYPL", "ADBE", "QCOM",
            "CSCO", "PEP", "KO", "DIS", "SBUX", "NKE"
    );

    // Cache for historical data and trained models
    private final Map<String, Map<String, Object>> dataCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    // private final Map<String, MultiLayerNetwork> modelCache = new ConcurrentHashMap<>();
    // private final Map<String, double[]> normalizationParams = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 30 * 60 * 1000; // 30 minutes

    private static final int SEQUENCE_LENGTH = 60; // 5 years of monthly data for training
    private static final int PREDICTION_HORIZON = 60; // Predict next 5 years (60 months)

    // ================= MAIN PREDICTION =================
    public StockPrediction predict(String stock) {
        try {
            // Fetch 10 years of historical data
            Map<String, Object> data = fetchHistoricalPrices(stock);
            List<Double> history = (List<Double>) data.get("prices");
            String latestDate = (String) data.get("latestDate");

            if (history.size() < SEQUENCE_LENGTH + 12) { // Need at least 5 years + 1 year for validation
                throw new RuntimeException("Not enough historical data for prediction. Need at least " +
                    (SEQUENCE_LENGTH + 12) + " months, got " + history.size());
            }

            double currentPrice = history.get(history.size() - 1);

            // Calculate historical volatility
            double historicalVolatility = calculateVolatility(history);

            // Predict future prices using simple growth model
            List<Double> futurePrices = new ArrayList<>();
            double avgGrowth = calculateAverageGrowth(history);
            for (int i = 0; i < PREDICTION_HORIZON; i++) {
                double growth = avgGrowth + (Math.random() * 0.02 - 0.01); // Add some randomness
                double nextPrice = currentPrice * Math.pow(1 + growth, (i + 1) / 12.0);
                futurePrices.add(Math.round(nextPrice * 100.0) / 100.0);
            }

            // Predict future volatility (assume it stays similar with slight adjustment)
            double predictedVolatility = historicalVolatility * (0.9 + Math.random() * 0.2); // 90-110% of historical

            // Calculate confidence based on data quality and volatility
            double confidence = Math.max(50.0, 100.0 - historicalVolatility * 100);

            // Calculate technical indicators
            double rsi = calculateRSI(history);
            double macd = calculateMACD(history);
            String marketSentiment = analyzeMarketSentiment(history);
            double dividendYield = estimateDividendYield(stock, currentPrice);
            double sharpeRatio = calculateSharpeRatio(history);

            return new StockPrediction(
                stock,
                currentPrice,
                latestDate,
                futurePrices,
                predictedVolatility,
                confidence,
                rsi,
                macd,
                marketSentiment,
                dividendYield,
                sharpeRatio,
                (boolean) data.getOrDefault("isMock", false),
                history
            );

        } catch (Exception e) {
            throw new RuntimeException("Prediction failed: " + e.getMessage(), e);
        }
    }

    // ================= DATA FETCHING =================
    private Map<String, Object> fetchHistoricalPrices(String symbol) {
        // Check cache first
        long currentTime = System.currentTimeMillis();
        if (dataCache.containsKey(symbol) && cacheTimestamps.containsKey(symbol)) {
            long cacheTime = cacheTimestamps.get(symbol);
            if (currentTime - cacheTime < CACHE_DURATION_MS) {
                return dataCache.get(symbol);
            }
        }

        try {
            List<Double> prices = new ArrayList<>();
            String latestDate = null;
            String apiUrl =
                    "https://www.alphavantage.co/query?function=TIME_SERIES_MONTHLY"
                            + "&symbol=" + symbol
                            + "&apikey=" + API_KEY;

            JSONObject json = readJson(apiUrl);

            // Check if we got rate limited or error responses
            if (json.has("Information") || json.has("Error Message") || !json.has("Monthly Time Series")) {
                System.out.println("Alpha Vantage API limit/error for " + symbol + ". Falling back to simulated data.");
                Map<String, Object> mockData = generateMockHistoricalPrices(symbol);
                dataCache.put(symbol, mockData);
                cacheTimestamps.put(symbol, currentTime);
                return mockData;
            }

            JSONObject series = json.getJSONObject("Monthly Time Series");
            List<String> dates = new ArrayList<>(series.keySet());
            Collections.sort(dates, Collections.reverseOrder()); // latest first

            int count = 0;
            for (String date : dates) {
                prices.add(series.getJSONObject(date).getDouble("4. close"));
                if (count == 0) latestDate = date;
                if (++count == 120) break; // Get 10 years (120 months) instead of 12
            }
            Collections.reverse(prices); // oldest first

            // Cache the data
            Map<String, Object> result = new HashMap<>();
            result.put("prices", prices);
            result.put("latestDate", latestDate);
            result.put("isMock", false);
            dataCache.put(symbol, result);
            cacheTimestamps.put(symbol, currentTime);
            return result;

        } catch (Exception e) {
            System.err.println("Failed to fetch online data for " + symbol + " due to: " + e.getMessage() + ". Falling back to mock data.");
            Map<String, Object> mockData = generateMockHistoricalPrices(symbol);
            dataCache.put(symbol, mockData);
            cacheTimestamps.put(symbol, currentTime);
            return mockData;
        }
    }

    private Map<String, Object> generateMockHistoricalPrices(String symbol) {
        List<Double> prices = new ArrayList<>();
        double currentPrice = switch (symbol) {
            case "AAPL" -> 210.0;
            case "GOOGL" -> 180.0;
            case "MSFT" -> 430.0;
            case "AMZN" -> 190.0;
            case "TSLA" -> 250.0;
            case "META" -> 500.0;
            case "NVDA" -> 120.0;
            case "NFLX" -> 650.0;
            case "IBM" -> 180.0;
            case "ORCL" -> 140.0;
            default -> 100.0;
        };

        // Generate 120 months of prices walking backwards
        java.time.LocalDate date = java.time.LocalDate.now().withDayOfMonth(1);
        String latestDate = date.toString();
        
        for (int i = 0; i < 120; i++) {
            prices.add(currentPrice);
            // Walk backwards randomly (with slight growth trend forwards, so backwards is decline)
            double change = (Math.random() * 0.05 - 0.02); 
            currentPrice = currentPrice / (1 + change);
            currentPrice = Math.round(currentPrice * 100.0) / 100.0;
        }
        Collections.reverse(prices); // oldest first
        
        Map<String, Object> result = new HashMap<>();
        result.put("prices", prices);
        result.put("latestDate", latestDate);
        result.put("isMock", true);
        return result;
    }

    // ================= MACHINE LEARNING METHODS =================
    /*
    private MultiLayerNetwork getOrTrainModel(String stock, List<Double> history) {
        if (modelCache.containsKey(stock)) {
            return modelCache.get(stock);
        }

        // Manual normalization
        double min = history.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = history.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        normalizationParams.put(stock, new double[]{min, max});

        // Normalize data
        List<Double> normalizedHistory = history.stream()
            .map(price -> (price - min) / (max - min))
            .toList();

        // Create sequences for training
        List<DataSet> dataSets = createTrainingData(normalizedHistory, SEQUENCE_LENGTH, 1);

        // Build LSTM model
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
            .seed(12345)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(0.001))
            .list()
            .layer(new LSTM.Builder()
                .nIn(1)
                .nOut(50)
                .activation(Activation.TANH)
                .build())
            .layer(new DenseLayer.Builder()
                .nOut(25)
                .activation(Activation.RELU)
                .build())
            .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nOut(1)
                .activation(Activation.IDENTITY)
                .build())
            .build();

        MultiLayerNetwork model = new MultiLayerNetwork(config);
        model.init();
        model.setListeners(new ScoreIterationListener(10));

        // Train the model
        for (int epoch = 0; epoch < 50; epoch++) {
            for (DataSet dataSet : dataSets) {
                model.fit(dataSet);
            }
        }

        modelCache.put(stock, model);
        return model;
    }

    private List<DataSet> createTrainingData(List<Double> data, int sequenceLength, int predictionLength) {
        List<DataSet> dataSets = new ArrayList<>();

        for (int i = 0; i < data.size() - sequenceLength - predictionLength + 1; i++) {
            // Input sequence
            List<Double> inputSeq = data.subList(i, i + sequenceLength);
            INDArray input = Nd4j.create(inputSeq.stream().mapToDouble(Double::doubleValue).toArray());
            input = input.reshape(1, 1, sequenceLength);

            // Output (next value)
            double outputVal = data.get(i + sequenceLength);
            INDArray output = Nd4j.create(new double[]{outputVal}).reshape(1, 1, 1);

            dataSets.add(new DataSet(input, output));
        }

        return dataSets;
    }

    private List<Double> generatePredictions(MultiLayerNetwork model, List<Double> history, int scenario) {
        List<Double> predictions = new ArrayList<>();

        // Get normalization parameters - we need to find the stock key
        // For now, let's assume we can get it from the model cache
        String stockKey = modelCache.entrySet().stream()
            .filter(entry -> entry.getValue() == model)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse("");

        double[] params = normalizationParams.get(stockKey);
        if (params == null) return predictions;

        double min = params[0];
        double max = params[1];

        // Use last SEQUENCE_LENGTH months for prediction
        List<Double> recentData = history.subList(Math.max(0, history.size() - SEQUENCE_LENGTH), history.size());

        // Normalize recent data
        List<Double> normalizedRecent = recentData.stream()
            .map(price -> (price - min) / (max - min))
            .toList();

        // Generate predictions month by month
        List<Double> currentSequence = new ArrayList<>(normalizedRecent);

        for (int i = 0; i < PREDICTION_HORIZON; i++) {
            // Prepare input
            double[] inputArray = currentSequence.stream().mapToDouble(Double::doubleValue).toArray();
            INDArray input = Nd4j.create(inputArray).reshape(1, 1, SEQUENCE_LENGTH);

            // Make prediction
            INDArray prediction = model.output(input);
            double normalizedPrediction = prediction.getDouble(0, 0, 0);

            // Denormalize prediction
            double predictedPrice = normalizedPrediction * (max - min) + min;

            // Apply scenario adjustment
            double scenarioMultiplier = 1.0 + (scenario * 0.1); // ±10% adjustment
            predictedPrice *= scenarioMultiplier;

            predictions.add(Math.round(predictedPrice * 100.0) / 100.0);

            // Update sequence for next prediction (sliding window)
            if (i < PREDICTION_HORIZON - 1) {
                currentSequence.remove(0);
                currentSequence.add(normalizedPrediction);
            }
        }

        return predictions;
    }
    */

    private String analyzeTrend(double currentPrice, List<Double> futurePrices) {
        if (futurePrices.isEmpty()) return "UNKNOWN";

        double avgFuturePrice = futurePrices.stream().mapToDouble(Double::doubleValue).average().orElse(currentPrice);
        double growthRate = (avgFuturePrice - currentPrice) / currentPrice;

        if (growthRate > 0.05) return "BULLISH 📈"; // >5% growth
        else if (growthRate < -0.05) return "BEARISH 📉"; // <-5% decline
        else return "SIDEWAYS ➡️"; // -5% to +5%
    }

    private String analyzeRisk(List<Double> history) {
        double volatility = calculateVolatility(history);

        if (volatility < 0.15) return "LOW"; // <15% volatility
        else if (volatility < 0.30) return "MEDIUM"; // 15-30% volatility
        else return "HIGH"; // >30% volatility
    }

    private double calculateConfidence(List<Double> history, List<Double> predictions) {
        double volatility = calculateVolatility(history);
        double predictionVariance = calculateVariance(predictions);

        // Confidence based on historical stability and prediction consistency
        double baseConfidence = Math.max(0.5, 1.0 - volatility);
        double predictionStability = Math.max(0.5, 1.0 - predictionVariance / predictions.get(0));

        return Math.round((baseConfidence * predictionStability * 100) * 100.0) / 100.0;
    }

    private double calculateVariance(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
    }

    // ================= VOLATILITY =================
    private double calculateVolatility(List<Double> prices) {
        if (prices.size() < 2) return 0.0;

        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            double ret = (prices.get(i) - prices.get(i-1)) / prices.get(i-1);
            returns.add(ret);
        }

        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double variance = returns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .average()
                .orElse(0);

        return Math.sqrt(variance); // Standard deviation of returns
    }

    private double calculateAverageGrowth(List<Double> prices) {
        if (prices.size() < 2) return 0.0;

        double totalGrowth = 0.0;
        int count = 0;
        for (int i = 1; i < prices.size(); i++) {
            double growth = (prices.get(i) - prices.get(i-1)) / prices.get(i-1);
            totalGrowth += growth;
            count++;
        }
        return totalGrowth / count; // Average monthly growth rate
    }

    public Set<String> getAllStocks() {
        return new HashSet<>(STOCKS);
    }

    // ================= JSON =================
    private JSONObject readJson(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();

        BufferedReader br;
        if (responseCode >= 200 && responseCode < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder json = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) json.append(line);
        br.close();

        return new JSONObject(json.toString());
    }

    // ================= TECHNICAL INDICATORS =================
    private double calculateRSI(List<Double> prices) {
        if (prices.size() < 14) return 50.0; // Default neutral RSI

        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();

        for (int i = 1; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i-1);
            if (change > 0) {
                gains.add(change);
                losses.add(0.0);
            } else {
                gains.add(0.0);
                losses.add(Math.abs(change));
            }
        }

        // Calculate average gains and losses (14-period)
        double avgGain = gains.subList(Math.max(0, gains.size() - 14), gains.size())
                         .stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgLoss = losses.subList(Math.max(0, losses.size() - 14), losses.size())
                         .stream().mapToDouble(Double::doubleValue).average().orElse(0);

        if (avgLoss == 0) return 100.0;

        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }

    private double calculateMACD(List<Double> prices) {
        if (prices.size() < 26) return 0.0;

        // Simple MACD calculation (12-period EMA - 26-period EMA)
        double ema12 = calculateEMA(prices, 12);
        double ema26 = calculateEMA(prices, 26);

        return ema12 - ema26;
    }

    private double calculateEMA(List<Double> prices, int period) {
        if (prices.size() < period) return prices.get(prices.size() - 1);

        double multiplier = 2.0 / (period + 1);
        double ema = prices.subList(0, period).stream().mapToDouble(Double::doubleValue).average().orElse(0);

        for (int i = period; i < prices.size(); i++) {
            ema = (prices.get(i) - ema) * multiplier + ema;
        }

        return ema;
    }

    private String analyzeMarketSentiment(List<Double> prices) {
        if (prices.size() < 12) return "NEUTRAL";

        // Analyze recent trend (last 12 months)
        List<Double> recent = prices.subList(Math.max(0, prices.size() - 12), prices.size());
        double startPrice = recent.get(0);
        double endPrice = recent.get(recent.size() - 1);
        double change = (endPrice - startPrice) / startPrice;

        if (change > 0.05) return "BULLISH 📈";
        else if (change < -0.05) return "BEARISH 📉";
        else return "NEUTRAL ➡️";
    }

    private double estimateDividendYield(String stock, double currentPrice) {
        // Simplified dividend yield estimation based on stock sector
        // In a real app, this would come from financial data APIs
        Map<String, Double> sectorYields = Map.of(
            "AAPL", 0.005, "GOOGL", 0.004, "MSFT", 0.007, "AMZN", 0.002,
            "TSLA", 0.001, "META", 0.003, "NVDA", 0.002, "NFLX", 0.001,
            "IBM", 0.045, "ORCL", 0.015
        );

        return sectorYields.getOrDefault(stock, 0.003); // Default 0.3%
    }

    private double calculateSharpeRatio(List<Double> prices) {
        if (prices.size() < 12) return 0.0;

        // Calculate monthly returns
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            returns.add((prices.get(i) - prices.get(i-1)) / prices.get(i-1));
        }

        double avgReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double volatility = calculateVolatility(prices);

        // Assume risk-free rate of 0.02 (2% annual)
        double riskFreeRate = 0.02 / 12; // Monthly

        if (volatility == 0) return 0.0;

        return (avgReturn - riskFreeRate) / volatility;
    }
}
