package io.mosip.idrepository.core.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class BytesToStringSerializer extends StdSerializer<byte[]> {

    public BytesToStringSerializer() {
        super(byte[].class);
    }

    protected BytesToStringSerializer(Class<byte[]> t) {
        super(t);
    }

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        int[] builder = new int[value.length];
        for(int i = 0; i < value.length; i++) {
            builder[i] = Integer.parseInt(Byte.toString(value[i]));
        }

        gen.writeArray(builder, 0, value.length);;
    }
}