package com.flashsale.inventory;

import com.flashsale.common.OrderStatus;
import com.flashsale.common.SaleStatus;
import com.flashsale.common.api.ChargeRequest;
import com.flashsale.common.api.SaleResponse;
import com.flashsale.inventory.client.CatalogClient;
import com.flashsale.inventory.client.PaymentClient;
import com.flashsale.inventory.domain.SaleStock;
import com.flashsale.inventory.redis.RedisStockService;
import com.flashsale.inventory.repository.OrderRepository;
import com.flashsale.inventory.repository.SaleStockRepository;
import com.flashsale.inventory.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.ResourceAccessException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PaymentTimeoutIntegrationTest {

    private static final UUID SALE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private SaleStockRepository saleStockRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RedisStockService redisStockService;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private CatalogClient catalogClient;

    @MockBean
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        saleStockRepository.deleteAll();

        when(catalogClient.getSale(SALE_ID)).thenReturn(new SaleResponse(
                SALE_ID,
                UUID.randomUUID(),
                "Sneakers",
                "Drop",
                BigDecimal.TEN,
                5,
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(3600),
                SaleStatus.ACTIVE
        ));
        when(paymentClient.charge(any(ChargeRequest.class)))
                .thenThrow(new ResourceAccessException("payment timeout"));

        SaleStock stock = new SaleStock();
        stock.setSaleId(SALE_ID);
        stock.setInitialStock(5);
        stock.setSold(0);
        saleStockRepository.save(stock);
        redisStockService.initializeStock(SALE_ID, 5);
    }

    @Test
    void paymentTimeoutCompensatesAndKeepsStockConsistent() {
        var order = orderService.placeOrder(new OrderService.CreateOrderCommand(
                SALE_ID, 1, "user@test.com", "payment-timeout-1"
        ));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        SaleStock stock = saleStockRepository.findById(SALE_ID).orElseThrow();
        assertThat(stock.getSold()).isZero();
        assertThat(redisStockService.getAvailableStock(SALE_ID)).isEqualTo(5);
    }
}
