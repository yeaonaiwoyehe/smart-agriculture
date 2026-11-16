package com.agri.cms.common.config;

import com.agri.cms.common.config.serializer.DebeziumMessageDeserializer;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.DebeziumMessage;
import jakarta.annotation.Resource;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-18 15:18
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-18 15:18
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;


    @Resource
    private TopicMappingProperties topicMappingProperties;

    @Bean
    public <T> ProducerFactory<String, DebeziumMessage<T>> debeziumProducerFactory() {
        Map<String, Object> configProps = new HashMap<>(8);
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 5);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public ConsumerFactory<String, DebeziumMessage<?>> debeziumConsumerFactory() {
        Map<String, Object> props = new HashMap<>(8);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "seaTunnel_subscribe");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, DebeziumMessageDeserializer.class);
        props.put(Constants.DEBEZIUM_TOPIC_MAPPING, topicMappingProperties.getMappings());
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DebeziumMessage<?>> debeziumListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DebeziumMessage<?>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(debeziumConsumerFactory());
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(debeziumKafkaTemplate(debeziumProducerFactory())),
            new FixedBackOff(1000L, 3L)));
        return factory;
    }

    @Bean
    public <T> KafkaTemplate<String, DebeziumMessage<T>> debeziumKafkaTemplate(ProducerFactory<String, DebeziumMessage<T>> debeziumProducerFactory) {
        return new KafkaTemplate<>(debeziumProducerFactory);
    }

}
