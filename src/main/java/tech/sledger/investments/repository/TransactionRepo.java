package tech.sledger.investments.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.investments.model.Transaction;
import java.util.List;

public interface TransactionRepo extends MongoRepository<Transaction, Integer> {
    Transaction findFirstByOrderByIdDesc();
    List<Transaction> findAllByInstrumentIdIsNotNullOrderByDate();
}
