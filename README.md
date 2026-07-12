# 🧠 Stock Intelligence Platform

An AI-powered financial forecasting dashboard and portfolio watchlist builder. The application is built using **Spring Boot 3.2.5** and **MongoDB**, utilizing historical month-end closing data to simulate a 5-Year future forecast connected directly with technical analytics.

---

## 🚀 Key Features

*   📈 **Dual-Line Historical & Forecast Charts**: Rendered via Chart.js, the visualizer overlays the **past 12 months of actual performance** alongside the **5-Year AI forecast**, connecting seamlessly at the current price.
*   🔑 **Lightweight Session Authentication**: High-performance, secure registration and login sessions using SHA-256 hashed password verification.
*   ⭐ **Database Watchlists**: Logged-in users can save their favorite tickers to their profile, displayed dynamically as rapid-access chips.
*   🧠 **Advanced Analytics & Technical Indicators**: Real-time evaluation of Relative Strength Index (RSI), MACD, Sharpe Ratio, Annual Volatility, and Market Sentiment (Bullish/Bearish).
*   🔄 **API Rate-Limit Fallback**: Auto-detects Alpha Vantage API rate-limiting or network issues, dynamically serving realistic mock simulations so the dashboard remains 100% functional.
*   ⚙️ **Credentials Isolation**: Isolates secrets (MongoDB URI and admin seeding details) into a separate properties configuration.

---

## 🛠️ Tech Stack

*   **Backend**: Java 17, Spring Boot 3 (Web, Data MongoDB)
*   **Database**: MongoDB
*   **Frontend**: Tailwind CSS, Chart.js, Vanilla HTML5/JS
*   **APIs**: Alpha Vantage (Historical Monthly Closing Prices)

---

## 📂 Quick Start & Setup

### 1. Prerequisites
*   Java 17
*   MongoDB running locally on `mongodb://localhost:27017/`

### 2. Configure Credentials
A template is provided in `demo/src/main/resources/credentials.properties.example`. Create a file named `credentials.properties` in the same directory:
```properties
# MongoDB connection
spring.data.mongodb.uri=mongodb://localhost:27017/stock_intelligence

# Default seeded account
default.user.username=nihar
default.user.password=nihar123
```
*Note: A startup database initializer will automatically seed the `nihar` user if it does not exist.*

### 3. Run the Server
Open your terminal in the `demo` directory and run:
```bash
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) to access the dashboard!
