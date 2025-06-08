package vn.fpt.seima.seimaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeimaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeimaServerApplication.class, args);
    }

}
