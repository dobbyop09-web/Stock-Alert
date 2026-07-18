package com.dobby.price_alert.dto;

public class StockMessageDto {
    private String stockName;
    private double currentPrice;
    private double targetPrice;
    private String screenerUrl;
    private String updateAlert;

    public static Builder builder() {
        return new Builder();
    }

    public String getUpdateAlert() {
        return updateAlert;
    }

    public void setUpdateAlert(String updateAlert) {
        this.updateAlert = updateAlert;
    }

    private StockMessageDto(Builder builder) {
        this.stockName = builder.stockName;
        this.currentPrice = builder.currentPrice;
        this.targetPrice = builder.targetPrice;
        this.screenerUrl = builder.screenerUrl;
        this.updateAlert = builder.updateAlert;
    }
    public static class Builder {

        private String stockName;
        private double currentPrice;
        private double targetPrice;
        private String screenerUrl;
        private String updateAlert;

        public Builder stockName(String stockName) {
            this.stockName = stockName;
            return this;
        }

        public Builder currentPrice(double currentPrice) {
            this.currentPrice = currentPrice;
            return this;
        }

        public Builder targetPrice(double targetPrice) {
            this.targetPrice = targetPrice;
            return this;
        }

        public Builder screenerUrl(String screenerUrl) {
            this.screenerUrl = screenerUrl;
            return this;
        }
        public Builder updateAlert (String updateAlert) {
            this.updateAlert = updateAlert;
            return this;
        }

        public StockMessageDto build() {
            return new StockMessageDto(this);
        }
    }
    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public String getScreenerUrl() {
        return screenerUrl;
    }

    public void setScreenerUrl(String screenerUrl) {
        this.screenerUrl = screenerUrl;
    }
}
