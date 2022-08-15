package tech.sledger.investments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.investments.model.*;
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
    private final TransactionRepo txRepo;
    private final PositionRepo positionRepo;
    private final InstrumentRepo instrumentRepo;
    private final TransactionService txService;

    @GetMapping("/recon")
    public String reconcilePositions() {
        log.info("Reconciling positions");
        positionRepo.deleteAll();

        List<Position> positions = new ArrayList<>();
        Map<Integer, List<Transaction>> transactions = txRepo.findAllByInstrumentIdIsNotNullOrderByDate()
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
                WorkingPosition w = txService.calculatePosition(thisTransactions);
                Position position = Position.builder()
                    .id(index++)
                    .instrument(instruments.get(instrumentId))
                    .position(positionSum)
                    .buyPrice(w.getTotalAmount().divide(w.getLatestPosition(), RoundingMode.HALF_EVEN).abs())
                    .buyFees(w.getTotalAmount().abs().subtract(w.getTotalNotionalAmount()))
                    .dividends(w.getDividends())
                    .buyFx(w.getTotalAmountLocal().divide(w.getTotalAmount(), RoundingMode.HALF_EVEN))
                    .build();
                positions.add(position);
            }
        }
        positionRepo.saveAll(positions);
        return "Done";
    }
}
