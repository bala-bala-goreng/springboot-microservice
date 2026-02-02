package com.boilerplate.app;

import com.boilerplate.app.base.logging.LoggingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@Slf4j
@SpringBootApplication
public class AuthenticationApplication {

	@Value("${spring.application.name:service-authentication}")
	private String serviceName;

	@PostConstruct
	public void init() {
		LoggingUtil.setServiceName(serviceName);
		log.info("Authentication Service started successfully");
	}

	public static void main(String[] args) {
		SpringApplication.run(AuthenticationApplication.class, args);
	}

}
