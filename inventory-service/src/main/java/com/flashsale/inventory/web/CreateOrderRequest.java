package com.flashsale.inventory.web;

import java.util.UUID;

public record CreateOrderRequest(UUID saleId, int quantity, String userEmail) {
}
