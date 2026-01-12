package com.alura.churnnsight;

import com.alura.churnnsight.config.FastApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
public class ChurnnsightApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChurnnsightApplication.class, args);
	}

}
