package org.yzh.web.config;


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
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.yzh.protocol.basics.JTMessage;
import org.yzh.protocol.t808.T0200;
import org.yzh.protocol.t808.T0801;
import org.yzh.web.Serializer.T0200Deserializer;
import org.yzh.web.Serializer.T0200Serializer;
import org.yzh.web.Serializer.T0801Deserializer;
import org.yzh.web.Serializer.T0801Serializer;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author simon
 * @since 2024/9/23 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.groups.t0200}")
    private String t0200GroupId;

    @Value("${spring.kafka.groups.t0801}")
    private String t0801GroupId;

    @Bean
    public ProducerFactory<String, T0200> t0200ProducerFactory() {
        Map<String, Object> configProps = new HashMap<>(8);
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, T0200Serializer.class);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 5);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public ProducerFactory<String, T0801> t0801ProducerFactory() {
        Map<String, Object> configProps = new HashMap<>(8);
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, T0801Serializer.class);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 5);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        return new DefaultKafkaProducerFactory<>(configProps);
    }


    @Bean
    public ProducerFactory<String, Object> commonProducerFactory() {
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
    public ConsumerFactory<String, T0200> t0200ConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, t0200GroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, T0200Deserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }
    @Bean
    public ConsumerFactory<String, T0801> t0801ConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, t0801GroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, T0801Deserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, T0200> t0200ListenerContainerFactory(ConsumerFactory<String, T0200> t0200ConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, T0200> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(t0200ConsumerFactory);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, T0801> t0801ListenerContainerFactory(ConsumerFactory<String, T0801> t0801ConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, T0801> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(t0801ConsumerFactory);
        return factory;
    }

    @Bean
    public  KafkaTemplate<String, T0200> t0200KafkaTemplate(ProducerFactory<String, T0200> t0200ProducerFactory) {
        return new KafkaTemplate<>(t0200ProducerFactory);
    }
        @Bean
    public  KafkaTemplate<String, T0801> t0801KafkaTemplate(ProducerFactory<String, T0801> t0801ProducerFactory) {
        return new KafkaTemplate<>(t0801ProducerFactory);
    }

    @Bean
    public KafkaTemplate<String, Object> commonProducer(ProducerFactory<String,Object> commonProducerFactory) {
        return new KafkaTemplate<>(commonProducerFactory);
    }
}