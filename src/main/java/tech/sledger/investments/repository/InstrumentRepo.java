package tech.sledger.investments.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.investments.model.Instrument;

public interface InstrumentRepo extends MongoRepository<Instrument, Integer> {
    Instrument findTopByName(String name);
    Instrument findFirstByOrderByIdDesc();
}
