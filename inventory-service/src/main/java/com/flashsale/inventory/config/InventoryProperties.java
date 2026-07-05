package com.flashsale.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory")
public class InventoryProperties {

    private long reserveTtlSeconds = 600;
    private final Outbox outbox = new Outbox();
    private final Expiry expiry = new Expiry();

    public long getReserveTtlSeconds() {
        return reserveTtlSeconds;
    }

    public void setReserveTtlSeconds(long reserveTtlSeconds) {
        this.reserveTtlSeconds = reserveTtlSeconds;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public Expiry getExpiry() {
        return expiry;
    }

    public static class Outbox {
        private long pollIntervalMs = 1000;

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }
    }

    public static class Expiry {
        private long pollIntervalMs = 5000;
        private long cutoffSeconds = 540;

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public long getCutoffSeconds() {
            return cutoffSeconds;
        }

        public void setCutoffSeconds(long cutoffSeconds) {
            this.cutoffSeconds = cutoffSeconds;
        }
    }
}
