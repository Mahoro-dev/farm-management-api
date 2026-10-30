package rw.farm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FarmManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarmManagementApplication.class, args);
    }
}
