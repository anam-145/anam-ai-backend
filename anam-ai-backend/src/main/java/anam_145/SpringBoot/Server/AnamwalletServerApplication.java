package anam_145.SpringBoot.Server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class AnamwalletServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnamwalletServerApplication.class, args);
	}

}
