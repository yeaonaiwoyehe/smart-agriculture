package cn.agrinav.front.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author 18506
 * 适配器类：用来处理LocalDateTime的序列化与反序列化
 */
public class GsonLocalDateTimeIsoAdapter extends TypeAdapter<LocalDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        if (value!= null) {
            out.value(FORMATTER.format(value));
        } else {
            out.nullValue();
        }
    }

    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String dateString = in.nextString();
        return LocalDateTime.parse(dateString, FORMATTER);
    }
}
