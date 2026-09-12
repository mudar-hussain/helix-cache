package com.mudar.helixcache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class HelixCacheApplication {
	public static void main(String[] args) {
		SpringApplication.run(HelixCacheApplication.class, args);
	}

}
