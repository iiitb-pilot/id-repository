package io.mosip.idrepository.core.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDateTime;

public class CustomLocalDateTimeDeSerializer extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        JsonNode date = node.get("date");
        JsonNode time = node.get("time");

        LocalDateTime localDateTime = LocalDateTime.of(date.get("year").intValue(), date.get("month").intValue(), date.get("day").intValue(), time.get("hour").intValue(), time.get("minute").intValue(), time.get("second").intValue(), time.get("nano").intValue());
        return localDateTime;
    }
}
