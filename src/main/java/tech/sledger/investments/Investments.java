package tech.sledger.investments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableRetry
@EnableCaching
@SpringBootApplication
public class Investments {
    public static void main(String[] args) {
        SpringApplication.run(Investments.class, args);
    }
}
