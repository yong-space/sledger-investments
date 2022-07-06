package tech.sledger.investments.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.investments.model.Config;

public interface ConfigRepo extends MongoRepository<Config, String> {
}
