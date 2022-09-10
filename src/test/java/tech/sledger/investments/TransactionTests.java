package tech.sledger.investments;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tech.sledger.investments.model.*;
import tech.sledger.investments.model.saxo.AssetType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tech.sledger.investments.model.TransactionType.*;

public class TransactionTests extends BaseTest {
    NewTransaction newTx = new NewTransaction(
        Trade,
        today,
        "1",
        "xyz",
        BigDecimal.TEN,
        BigDecimal.TEN,
        BigDecimal.valueOf(105)
    );

    NewTransaction closeTx = new NewTransaction(
        Trade,
        today,
        "1",
        "xyz",
        BigDecimal.TEN,
        BigDecimal.valueOf(-10),
        BigDecimal.valueOf(105)
    );

    NewTransaction dividendTx = new NewTransaction(
        TransactionType.Dividends,
        Instant.now(),
        "1",
        "xyz",
        null,
        null,
        BigDecimal.valueOf(105)
    );

    @Test
    public void listTx() throws Exception {
        mvc.perform(get("/tx")).andExpect(status().isOk());
    }

    @Test
    public void getBadTx() throws Exception {
        mvc.perform(get("/tx/5678")).andExpect(status().isNotFound());
    }

    @Test
    public void addTxNewInstrument() throws Exception {
        String txString = mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(newTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String txId = JsonPath.parse(txString).read("$.id").toString();

        mvc.perform(get("/portfolio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("xyz"))
            .andExpect(jsonPath("$[0].name").value("Hello"))
            .andDo(result -> {
                String json = result.getResponse().getContentAsString();
                List<PortfolioEntry> entries = objectMapper.readValue(json, new TypeReference<>() {});
                PortfolioEntry entry = entries.get(0);
                assertEquals(0, entry.getPosition().compareTo(BigDecimal.TEN));
                assertEquals(0, entry.getPrice().compareTo(BigDecimal.TEN));
                assertEquals(0, entry.getAmount().compareTo(BigDecimal.valueOf(105)));
                assertEquals(0, entry.getDividends().compareTo(BigDecimal.ZERO));
            });

        mvc.perform(delete("/tx/" + txId))
            .andExpect(status().isOk());
    }

    @Test
    public void addDividendTxWithoutPosition() throws Exception {
        mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(dividendTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void addDividendTxWithPosition() throws Exception {
        mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(newTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(dividendTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(get("/portfolio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andDo(result -> {
                String json = result.getResponse().getContentAsString();
                List<PortfolioEntry> entries = objectMapper.readValue(json, new TypeReference<>() {});
                PortfolioEntry entry = entries.get(0);
                assertEquals(0, entry.getDividends().compareTo(BigDecimal.valueOf(105)));
            });

        txRepo.deleteAll();
        positionRepo.deleteAll();
    }

    @Test
    public void addTxClosePosition() throws Exception {
        mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(newTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(closeTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(get("/portfolio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        txRepo.deleteAll();
        positionRepo.deleteAll();
    }

    @Test
    public void addTxExistingInstrument() throws Exception {
        mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(newTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(newTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(get("/portfolio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("xyz"))
            .andExpect(jsonPath("$[0].name").value("Hello"))
            .andDo(result -> {
                String json = result.getResponse().getContentAsString();
                List<PortfolioEntry> entries = objectMapper.readValue(json, new TypeReference<>() {});
                PortfolioEntry entry = entries.get(0);
                assertEquals(0, entry.getPosition().compareTo(BigDecimal.valueOf(20)));
                assertEquals(0, entry.getPrice().compareTo(BigDecimal.TEN));
                assertEquals(0, entry.getAmount().compareTo(BigDecimal.valueOf(210)));
                assertEquals(0, entry.getDividends().compareTo(BigDecimal.ZERO));
            });

        txRepo.deleteAll();
        positionRepo.deleteAll();
    }

    @Test
    public void addInterestTx() throws Exception {
        NewTransaction interestTx = new NewTransaction(
            TransactionType.Interest,
            Instant.now(),
            "SYEP",
            "SYEP",
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.valueOf(105)
        );

        String txString = mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(interestTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String txId = JsonPath.parse(txString).read("$.id").toString();

        mvc.perform(get("/tx/" + txId))
            .andExpect(status().isOk());

        mvc.perform(delete("/tx/" + txId))
            .andExpect(status().isOk());
    }

    @Test
    public void addDepositTx() throws Exception {
        NewTransaction depositTx = new NewTransaction(
            Deposit,
            Instant.now(),
            null,
            null,
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.valueOf(105)
        );

        String txString = mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(depositTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String txId = JsonPath.parse(txString).read("$.id").toString();

        mvc.perform(get("/tx/" + txId))
            .andExpect(status().isOk());

        mvc.perform(delete("/tx/" + txId))
            .andExpect(status().isOk());
    }

    @Test
    public void addFeesTx() throws Exception {
        NewTransaction feesTx = new NewTransaction(
            TransactionType.Fees,
            Instant.now(),
            null,
            null,
            BigDecimal.TEN,
            BigDecimal.TEN,
            BigDecimal.valueOf(105)
        );

        String txString = mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(feesTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String txId = JsonPath.parse(txString).read("$.id").toString();

        mvc.perform(get("/tx/" + txId))
            .andExpect(status().isOk());

        mvc.perform(delete("/tx/" + txId))
            .andExpect(status().isOk());
    }

    @Test
    public void deleteTxInvalid() throws Exception {
        mvc.perform(delete("/tx/12345"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteTxClosePosition() throws Exception {
        mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(newTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(delete("/tx/1"))
            .andExpect(status().isOk());

        mvc.perform(get("/portfolio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void deleteTxModifyPosition() throws Exception {
        mvc.perform(post("/tx")
            .content(objectMapper.writeValueAsString(newTx))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(post("/tx")
                .content(objectMapper.writeValueAsString(newTx))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(delete("/tx/1"))
            .andExpect(status().isOk());

        mvc.perform(get("/portfolio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andDo(result -> {
                String json = result.getResponse().getContentAsString();
                List<PortfolioEntry> entries = objectMapper.readValue(json, new TypeReference<>() {});
                PortfolioEntry entry = entries.get(0);
                assertEquals(0, entry.getPosition().compareTo(BigDecimal.TEN));
                assertEquals(0, entry.getPrice().compareTo(BigDecimal.TEN));
                assertEquals(0, entry.getAmount().compareTo(BigDecimal.valueOf(105)));
                assertEquals(0, entry.getDividends().compareTo(BigDecimal.ZERO));
            });
    }

    @Test
    public void calculatePosition() {
        List<Transaction> txList = List.of(
            Transaction.builder().type(Trade).fxRate(one).price(one).amount(neg).date(yesterday).instrumentId(1).quantity(one).build(),
            Transaction.builder().type(Dividends).fxRate(one).price(one).amount(neg).date(yesterday).instrumentId(1).quantity(one).build(),
            Transaction.builder().type(Deposit).fxRate(one).price(one).amount(neg).date(yesterday).instrumentId(1).quantity(one).build(),
            Transaction.builder().type(Fees).fxRate(one).price(one).amount(neg).date(yesterday).instrumentId(1).quantity(one).build(),
            Transaction.builder().type(Interest).fxRate(one).price(one).amount(neg).date(yesterday).instrumentId(1).quantity(one).build(),
            Transaction.builder().type(Trade).fxRate(one).price(one).amount(one).date(Instant.now()).instrumentId(1).quantity(neg).build()
        );
        WorkingPosition p = txService.calculatePosition(txList);

        assertEquals(0, p.getLatestPosition().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void getFxRateNonUsd() {
        assertEquals(0, txService.getFxRate("HKD", Instant.now()).compareTo(BigDecimal.ONE));
    }

    @Test
    public void getFxRatePriorDeposit() {
        txRepo.save(Transaction.builder().type(Deposit).fxRate(one).price(BigDecimal.TEN).amount(neg).date(yesterday).instrumentId(1).quantity(one).build());
        assertEquals(0, txService.getFxRate("USD", Instant.now()).compareTo(BigDecimal.TEN));
        txRepo.deleteAll();
    }

    @Test
    public void recon() throws Exception {
        instrumentRepo.save(Instrument.builder().id(1).assetType(AssetType.Stock).currency("USD").build());
        instrumentRepo.save(Instrument.builder().id(2).assetType(AssetType.Stock).currency("USD").build());
        txRepo.save(Transaction.builder().id(1).type(Trade).fxRate(one).price(one).amount(neg).date(yesterday).instrumentId(2).quantity(one).build());
        txRepo.save(Transaction.builder().id(2).type(Trade).fxRate(one).price(one).amount(neg).date(today).instrumentId(2).quantity(neg).build());
        txRepo.save(Transaction.builder().id(3).type(Trade).fxRate(one).price(one).amount(neg).date(today).instrumentId(1).quantity(one).build());
        txRepo.save(Transaction.builder().id(4).type(Deposit).fxRate(one).price(one).amount(neg).date(today).instrumentId(1).quantity(one).build());

        mvc.perform(get("/recon"))
            .andExpect(status().isOk());

        mvc.perform(get("/portfolio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andDo(result -> {
                String json = result.getResponse().getContentAsString();
                List<PortfolioEntry> entries = objectMapper.readValue(json, new TypeReference<>() {});
                PortfolioEntry entry = entries.get(0);
                assertEquals(0, entry.getPosition().compareTo(one));
                assertEquals(0, entry.getPrice().compareTo(BigDecimal.TEN));
                assertEquals(0, entry.getAmount().compareTo(one));
                assertEquals(0, entry.getDividends().compareTo(BigDecimal.ZERO));
            });

        txRepo.deleteAll();
        positionRepo.deleteAll();
        instrumentRepo.deleteAll();
    }
}
