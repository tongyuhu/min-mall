package com.minimall.common.util;
import tools.jackson.databind.ObjectMapper; import org.springframework.stereotype.Component;
@Component public class Json {
    private final ObjectMapper mapper; public Json(ObjectMapper mapper){this.mapper=mapper;}
    public String write(Object value){return mapper.writeValueAsString(value);}
    public <T>T read(String value,Class<T> type){return mapper.readValue(value,type);}
}
