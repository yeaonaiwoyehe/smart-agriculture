package org.yzh.web.Serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.apache.kafka.common.serialization.Deserializer;
import org.yzh.protocol.basics.JTMessage;
import org.yzh.protocol.t808.T0200;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author simon
 * @since 2024/11/16 */

public class T0200Deserializer implements Deserializer<T0200> {
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, context) -> LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .registerTypeAdapter(LocalDate.class, (com.google.gson.JsonSerializer<LocalDate>) (src, type, context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
            .registerTypeAdapter(LocalDate.class, (com.google.gson.JsonDeserializer<LocalDate>) (json, type, context) -> LocalDate.parse(json.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
            .registerTypeAdapter(LocalTime.class, (com.google.gson.JsonSerializer<LocalTime>) (src, type, context) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_TIME)))
            .registerTypeAdapter(LocalTime.class, (com.google.gson.JsonDeserializer<LocalTime>) (json, type, context) -> LocalTime.parse(json.getAsJsonPrimitive().getAsString(), DateTimeFormatter.ISO_LOCAL_TIME))
            .registerTypeAdapter(ByteBuf.class, (com.google.gson.JsonSerializer<ByteBuf>) (src, type, context) -> new JsonPrimitive(ByteBufUtil.hexDump(src)))
            .registerTypeAdapter(ByteBuf.class, (com.google.gson.JsonDeserializer<ByteBuf>) (json, type, context) -> Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(json.getAsJsonPrimitive().getAsString())))
            .setPrettyPrinting()
            .create();

    @Override
    public T0200 deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        String json = new String(data, StandardCharsets.UTF_8);
        return gson.fromJson(json, T0200.class);
    }

}
