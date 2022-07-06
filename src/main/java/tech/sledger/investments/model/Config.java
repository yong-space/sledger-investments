package tech.sledger.investments.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@Builder
public class Config {
    @Id
    private String key;
    private String value;
}
