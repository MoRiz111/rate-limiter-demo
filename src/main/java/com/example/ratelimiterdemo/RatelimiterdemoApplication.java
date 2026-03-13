package com.example.ratelimiterdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RatelimiterdemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(RatelimiterdemoApplication.class, args);
	}

}
