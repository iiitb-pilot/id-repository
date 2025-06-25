package io.mosip.idrepository.core.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MapperUtil {

    private ObjectMapper mapper;

    public ObjectMapper getObjectMapper() {
        if(mapper == null) {
            mapper = new ObjectMapper().registerModule(new AfterburnerModule());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeSerializer());
            module.addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer());
            module.addSerializer(byte[].class, new BytesToStringSerializer());
            mapper.registerModule(module);
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        return mapper;
    }
}
