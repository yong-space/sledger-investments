package tech.sledger.investments;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import tech.sledger.investments.client.SaxoClient;
import tech.sledger.investments.model.saxo.*;
import tech.sledger.investments.repository.InstrumentRepo;
import tech.sledger.investments.repository.PositionRepo;
import tech.sledger.investments.repository.TransactionRepo;
import tech.sledger.investments.service.TransactionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class BaseTest {
    @DynamicPropertySource
    public static void setDatasourceProperties(final DynamicPropertyRegistry registry) {
        MongoDBContainer container = new MongoDBContainer("mongo:5.0.8");
        container.start();
        registry.add("spring.data.mongodb.uri", container::getReplicaSetUrl);
    }

    @Autowired
    public MockMvc mvc;
    @Autowired
    public ObjectMapper objectMapper;
    @MockBean
    public SaxoClient saxoClient;
    @Autowired
    public TransactionRepo txRepo;
    @Autowired
    public PositionRepo positionRepo;
    @Autowired
    public TransactionService txService;
    @Autowired
    public InstrumentRepo instrumentRepo;

    BigDecimal one = BigDecimal.ONE;
    BigDecimal neg = BigDecimal.valueOf(-1);
    Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
    Instant today = Instant.now();

    SearchResults dummyInstrument = new SearchResults(List.of(
        new RawInstrument("Stock", "USD", "Hello", "xyz", 1, 1, "US", 1, "Hello", "xyz", List.of()),
        new RawInstrument("FxSpot", "USD", "USD/SGD", "xyz", 2, 45, "US", 1, "USD/SGD", "usdsgd", List.of())
    ));
    PriceQuote stockQuote = new PriceQuote(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
    PriceInfo stockPriceInfo = new PriceInfo(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO);
    PriceResponse dummyStockPrice = new PriceResponse(List.of(new PriceEntry(Instant.now(), 1, stockQuote, stockPriceInfo, new PriceInfoDetails(BigDecimal.TEN))));
    PriceQuote fxQuote = new PriceQuote(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
    PriceInfo fxPriceInfo = new PriceInfo(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);
    PriceResponse dummyFxPrice = new PriceResponse(List.of(new PriceEntry(Instant.now(), 45, fxQuote, fxPriceInfo, new PriceInfoDetails(BigDecimal.ONE))));

    @PostConstruct
    public void init() {
        when(saxoClient.searchInstruments(anyList())).thenReturn(dummyInstrument);
        when(saxoClient.getPrices(eq(AssetType.Stock), anyList())).thenReturn(dummyStockPrice);
        when(saxoClient.getPrices(eq(AssetType.FxSpot), anyList())).thenReturn(dummyFxPrice);
    }
}
