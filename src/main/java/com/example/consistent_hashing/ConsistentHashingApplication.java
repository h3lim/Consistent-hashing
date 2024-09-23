package com.example.consistent_hashing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class ConsistentHashingApplication {
	public static void main(String[] args) {
		SpringApplication.run(ConsistentHashingApplication.class, args);
	}
}
