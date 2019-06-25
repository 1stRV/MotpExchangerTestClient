package ru.x5.motpsender.testclient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import ru.x5.motpsender.testclient.controller.MotpToken;

@SpringBootApplication
public class MotpSenderTestClient {


	@Autowired
	ConsumerFactory consumerFactory;

	@Autowired
	ProducerFactory producerFactory;

	@Autowired
	KafkaMessageListenerContainer kafkaMessageListenerContainer;

	@Bean
	public KafkaMessageListenerContainer<String, MotpToken> replyListenerContainer() {
		ContainerProperties containerProperties = new ContainerProperties("tokenOut", "aggregatedCisOut","productsOut");
		return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
	}

	@Bean
	public ReplyingKafkaTemplate<String, MotpToken, MotpToken> replyKafkaTemplate() {
		return new ReplyingKafkaTemplate<>(producerFactory, kafkaMessageListenerContainer);
	}

	public static void main(String[] args) {
		SpringApplication.run(MotpSenderTestClient.class, args);
	}
}