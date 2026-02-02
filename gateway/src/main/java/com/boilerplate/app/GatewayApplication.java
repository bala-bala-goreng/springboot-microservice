package com.boilerplate.app;

import com.boilerplate.app.base.logging.LoggingUtil;
import com.boilerplate.app.config.GatewayRouteConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(GatewayRouteConfig.class)
public class GatewayApplication {

	public static void main(String[] args) {
		LoggingUtil.setServiceName("service-gateway");
		SpringApplication.run(GatewayApplication.class, args);
		log.info("Gateway Service started successfully");
	}

}
