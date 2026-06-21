package it.unibs.ingsw24_25;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TourGuideApplication {

    public static void main(String[] args) {
        SpringApplication.run(TourGuideApplication.class, args);
    }

}
