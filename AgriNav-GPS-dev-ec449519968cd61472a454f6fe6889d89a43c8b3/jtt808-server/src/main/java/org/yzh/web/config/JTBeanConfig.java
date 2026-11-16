package org.yzh.web.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import io.github.yezhihao.netmc.core.HandlerMapping;
import io.github.yezhihao.netmc.core.SpringHandlerMapping;
import io.github.yezhihao.netmc.session.SessionListener;
import io.github.yezhihao.netmc.session.SessionManager;
import io.github.yezhihao.protostar.SchemaManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yzh.protocol.basics.JTMessage;
import org.yzh.protocol.codec.DataFrameMessageDecoder;
import org.yzh.protocol.codec.JTMessageAdapter;
import org.yzh.protocol.codec.JTMessageEncoder;
import org.yzh.protocol.codec.MultiPacketDecoder;
import org.yzh.web.endpoint.JTHandlerInterceptor;
import org.yzh.web.endpoint.JTMultiPacketListener;
import org.yzh.web.endpoint.JTSessionListener;
import org.yzh.web.model.enums.SessionKey;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JTBeanConfig {

    @Bean
    public HandlerMapping handlerMapping() {
        return new SpringHandlerMapping();
    }

    @Bean
    public JTHandlerInterceptor handlerInterceptor() {
        return new JTHandlerInterceptor();
    }

    @Bean
    public SessionListener sessionListener() {
        return new JTSessionListener();
    }

    @Bean
    public SessionManager sessionManager(SessionListener sessionListener) {
        return new SessionManager(SessionKey.class, sessionListener);
    }

    @Bean
    public SchemaManager schemaManager() {
        return new SchemaManager("org.yzh.protocol");
    }

    @Bean
    public JTMessageAdapter messageAdapter(SchemaManager schemaManager) {
        JTMessageEncoder encoder = new JTMessageEncoder(schemaManager);
        MultiPacketDecoder decoder = new MultiPacketDecoder(schemaManager, new JTMultiPacketListener(10));
        return new WebLogAdapter(encoder, decoder);
    }

    @Bean
    public JTMessageAdapter alarmFileMessageAdapter(SchemaManager schemaManager) {
        JTMessageEncoder encoder = new JTMessageEncoder(schemaManager);
        DataFrameMessageDecoder decoder = new DataFrameMessageDecoder(schemaManager, new byte[]{0x30, 0x31, 0x63, 0x64});
        return new WebLogAdapter(encoder, decoder);
    }

    @Bean
    public MultiPacketDecoder multiPacketDecoder(SchemaManager schemaManager) {
        return new MultiPacketDecoder(schemaManager);
    }
}