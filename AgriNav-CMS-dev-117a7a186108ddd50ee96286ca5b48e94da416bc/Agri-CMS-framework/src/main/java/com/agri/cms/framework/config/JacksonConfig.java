package com.agri.cms.framework.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.postgis.LineString;
import org.postgis.Point;
import org.postgis.Polygon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {


    @Bean
    public ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        javaTimeModule.addSerializer(LocalDateTime.class,
//            new LocalDateTimeSerializer(formatter));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        javaTimeModule.addDeserializer(LocalDateTime.class,
//            new LocalDateTimeDeserializer(formatter));
        SimpleModule module = new SimpleModule();
        mapper.registerModule(javaTimeModule);
        module.addSerializer(Polygon.class, new GeometrySerializer.PolygonSerializer());
        module.addSerializer(Point.class, new GeometrySerializer.PointSerializer());
        module.addSerializer(LineString.class, new GeometrySerializer.LineStringSerializer());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.registerModule(module);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }


}
