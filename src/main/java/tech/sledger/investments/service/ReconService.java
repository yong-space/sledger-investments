package tech.sledger.investments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.investments.model.Instrument;
import tech.sledger.investments.model.Position;
import tech.sledger.investments.model.Transaction;
import tech.sledger.investments.model.TransactionType;
import tech.sledger.investments.repository.InstrumentRepo;
import tech.sledger.investments.repository.PositionRepo;
import tech.sledger.investments.repository.TransactionRepo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ReconService {
    private final TransactionRepo transactionRepo;
    private final PositionRepo positionRepo;
    private final InstrumentRepo instrumentRepo;

    @GetMapping("/recon")
    public String reconcilePositions() {
        log.info("Reconciling positions");
        positionRepo.deleteAll();

        List<Position> positions = new ArrayList<>();
        Map<Integer, List<Transaction>> transactions = transactionRepo.findAllByInstrumentIdIsNotNullOrderByDate()
            .stream().collect(Collectors.groupingBy(Transaction::getInstrumentId));
        Map<Integer, Instrument> instruments = instrumentRepo.findAll().stream()
            .collect(Collectors.toMap(Instrument::getId, i -> i));

        int index = 1;
        for (int instrumentId : transactions.keySet()) {
            List<Transaction> thisTransactions = transactions.get(instrumentId);
            BigDecimal positionSum = thisTransactions.stream()
                .filter(t -> t.getType() == TransactionType.Trade)
                .map(Transaction::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (positionSum.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal latestPosition = BigDecimal.ZERO;
                BigDecimal totalPrice = BigDecimal.ZERO;
                BigDecimal totalAmount = BigDecimal.ZERO;
                BigDecimal totalAmountLocal = BigDecimal.ZERO;
                BigDecimal totalNotionalAmount = BigDecimal.ZERO;
                BigDecimal dividends = BigDecimal.ZERO;

                for (Transaction t : thisTransactions) {
                    switch (t.getType()) {
                        case Trade -> {
                            latestPosition = latestPosition.add(t.getQuantity());
                            if (latestPosition.equals(BigDecimal.ZERO)) {
                                totalPrice = BigDecimal.ZERO;
                                totalAmount = BigDecimal.ZERO;
                                totalAmountLocal = BigDecimal.ZERO;
                                totalNotionalAmount = BigDecimal.ZERO;
                                dividends = BigDecimal.ZERO;
                            } else {
                                totalAmount = totalAmount.add(t.getAmount());
                                totalAmountLocal = totalAmountLocal.add(t.getAmount().multiply(t.getFxRate()));
                                totalPrice = totalPrice.add(t.getPrice());
                                totalNotionalAmount = totalNotionalAmount.add(t.getPrice().multiply(t.getQuantity()));
                            }
                        }
                        case Dividends -> {
                            dividends = dividends.add(t.getAmount());
                        }
                    }
                }

                Position position = Position.builder()
                    .id(index++)
                    .instrument(instruments.get(instrumentId))
                    .position(positionSum)
                    .buyPrice(totalAmount.divide(latestPosition, RoundingMode.HALF_EVEN).abs())
                    .buyFees(totalAmount.abs().subtract(totalNotionalAmount))
                    .dividends(dividends)
                    .buyFx(totalAmountLocal.divide(totalAmount, RoundingMode.HALF_EVEN))
                    .build();
                positions.add(position);
            }
        }
        positionRepo.saveAll(positions);
        return "Done";
    }
}
