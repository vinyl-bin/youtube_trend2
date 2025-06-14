package com.cookiek.commenthat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@EnableScheduling
public class CommenthatApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommenthatApplication.class, args);
	}

	@Bean
	public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
		return new TransactionTemplate(transactionManager);
	}

}
