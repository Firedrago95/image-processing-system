package com.realteeth.assignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class ImageProcessingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImageProcessingSystemApplication.class, args);
	}

}
