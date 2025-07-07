package vn.fpt.seima.seimaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration.class
})
@EnableScheduling
public class SeimaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeimaServerApplication.class, args);
    }

}
