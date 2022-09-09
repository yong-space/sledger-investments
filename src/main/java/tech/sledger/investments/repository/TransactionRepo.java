package tech.sledger.investments.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.investments.model.Transaction;
import tech.sledger.investments.model.TransactionType;
import java.time.Instant;
import java.util.List;

public interface TransactionRepo extends MongoRepository<Transaction, Integer> {
    Transaction findFirstByOrderByIdDesc();
    Transaction findFirstByTypeAndDateLessThanEqualAndPriceIsNotNullOrderByDateDesc(TransactionType type, Instant date);
    Transaction findFirstByTypeAndDateGreaterThanAndPriceIsNotNullOrderByDate(TransactionType type, Instant date);
    List<Transaction> findAllByInstrumentIdIsNotNullOrderByDate();
    List<Transaction> findAllByTypeOrderByDate(TransactionType type);
    List<Transaction> findAllByInstrumentIdOrderByDate(Integer instrumentId);
    List<Transaction> findAllByOrderByDateDesc();
}
