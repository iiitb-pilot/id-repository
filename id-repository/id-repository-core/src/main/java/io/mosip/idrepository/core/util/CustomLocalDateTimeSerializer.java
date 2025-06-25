package io.mosip.idrepository.core.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;

public class CustomLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeObjectFieldStart("date");
        gen.writeNumberField("year", value.getYear());
        gen.writeNumberField("month", value.getMonthValue());
        gen.writeNumberField("day", value.getDayOfMonth());
        gen.writeEndObject();

        gen.writeObjectFieldStart("time");
        gen.writeNumberField("hour", value.getHour());
        gen.writeNumberField("minute", value.getMinute());
        gen.writeNumberField("second", value.getSecond());
        gen.writeNumberField("nano", value.getNano());
        gen.writeEndObject();

        gen.writeEndObject();
    }
}