package com.flashsale.inventory;

import com.flashsale.inventory.client.CatalogClient;
import com.flashsale.inventory.client.PaymentClient;
import com.flashsale.inventory.redis.RedisStockService;
import com.flashsale.inventory.repository.OrderRepository;
import com.flashsale.inventory.repository.SaleStockRepository;
import com.flashsale.inventory.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected OrderService orderService;

    @Autowired
    protected SaleStockRepository saleStockRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected RedisStockService redisStockService;

    @MockBean
    protected KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    protected CatalogClient catalogClient;

    @MockBean
    protected PaymentClient paymentClient;
}
