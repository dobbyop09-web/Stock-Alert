# 📈 Stock Alert Dashboard

A personal stock monitoring and alert system for the Indian stock market built with **Spring Boot**, **PostgreSQL**, **GitHub Actions**, and a lightweight **GitHub Pages dashboard**.

The application periodically fetches live market data, checks user-defined price alerts, sends Telegram notifications, and publishes an interactive dashboard that can be viewed from anywhere.

---

## ✨ Features

### 📊 Dashboard
- Interactive stock dashboard hosted on GitHub Pages
- Sector-wise filtering
- Search by symbol
- Market Cap sorting
- Sort by distance from alert price
- Status filters
  - Triggered
  - Near Alert
  - Watch
- Heatmap visualization
- Responsive mobile-friendly layout
- Last update timestamp
- Manual refresh button

---

### 🔔 Price Alerts

- Configure alert prices for stocks
- Telegram notifications when targets are reached
- Prevents duplicate notifications using cooldown logic
- Edit alert prices directly from the dashboard
- Google Sheets integration for updating alerts

---

### 📈 Market Data

- Fetches latest market prices
- Stores historical daily candles
- Tracks:
  - Current Price
  - Previous Close
  - Alert Price
  - FIB Level
  - Market Capitalization
- Generates dashboard JSON automatically

---

### ☁️ Automation

- Runs entirely on GitHub Actions
- Scheduled market refreshes
- Manual refresh from dashboard
- Cloudflare Worker acts as a secure trigger endpoint
- Automatic cooldown to prevent duplicate runs

---

## 🏗️ Architecture

```text
                 GitHub Pages
                       │
                Dashboard UI
                       │
         dashboard-data.json
         dashboard-meta.json
                       ▲
                       │
                GitHub Actions
                       │
                Spring Boot App
                       │
     ┌─────────────────┴────────────────┐
     │                                  │
 PostgreSQL                     Google Sheets
     │                                  │
     └────────── Market Data ───────────┘
                       │
                  Telegram Bot
```

---

## 🛠️ Tech Stack

### Backend

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Jackson

### Frontend

- HTML
- CSS
- Vanilla JavaScript
- D3.js (Heatmap)

### Automation

- GitHub Actions
- Cloudflare Workers

### Integrations

- Google Sheets API
- Telegram Bot API

---

## 📂 Project Structure

```
src/
 ├── controller/
 ├── service/
 ├── repository/
 ├── entity/
 ├── dto/
 ├── mapper/
 └── config/

dashboard/
 ├── dashboard-data.json
 ├── dashboard-meta.json
 └── index.html
```

---

## 🚀 Workflow

1. GitHub Actions starts on schedule or manual trigger.
2. Spring Boot application runs.
3. Latest stock data is fetched.
4. Alerts are evaluated.
5. Telegram notifications are sent.
6. Dashboard JSON files are regenerated.
7. Changes are committed back to GitHub.
8. GitHub Pages instantly serves the latest dashboard.

---

## 📷 Dashboard

Features include:

- Live market overview
- Sector filtering
- Search
- Interactive heatmap
- Inline editing of alert values
- Google Sheets synchronization
- Manual refresh

*(Screenshots/GIFs coming soon)*

---

## 🔮 Planned Features

- Portfolio Tracker
- P&L Dashboard
- Historical Charts
- Multi-user portfolios
- Custom Watchlists
- Trade Journal
- Portfolio allocation
- Performance analytics

---

## ⚙️ Running Locally

Clone the repository

```bash
git clone https://github.com/dobbyop09-web/Stock-Alert.git
```

Configure environment variables:

```
DB_URL=
DB_USERNAME=
DB_PASSWORD=
BOT_TOKEN=
CHAT_IDS=
```

Run the application

```bash
mvn spring-boot:run
```

Open the dashboard

```
dashboard/index.html
```

---

## 📌 Motivation

This project started as a personal tool to avoid constantly checking market prices throughout the day. Instead of watching charts continuously, I wanted a system that automatically monitors stocks, sends alerts, and presents everything in a simple dashboard.

Over time it evolved into a complete stock monitoring platform with automation, visualization, and editing capabilities.

---

## 🤝 Contributions

This project is currently a personal learning project, but suggestions and ideas are always welcome.

If you have ideas for improving the dashboard or adding new features, feel free to open an issue.

---
