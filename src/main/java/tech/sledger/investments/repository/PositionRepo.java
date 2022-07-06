package tech.sledger.investments.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.investments.model.Position;

public interface PositionRepo extends MongoRepository<Position, Integer> {
}
