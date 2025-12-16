package com.example.springboot.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Configuration
public class JacksonConfig {

    // 定义ISO 8601格式
    private static final String ISO_8601_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 配置日期和时间的序列化和反序列化
        SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_DATE_TIME_FORMAT);
        objectMapper.setDateFormat(dateFormat);
        
        // 禁用默认的时间戳格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 注册Java 8时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(ISO_8601_DATE_TIME_FORMAT);
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        
        // 为java.sql.Timestamp添加专门的序列化器和反序列化器
        SimpleModule timestampModule = new SimpleModule();
        timestampModule.addSerializer(Timestamp.class, new StdSerializer<Timestamp>(Timestamp.class) {
            @Override
            public void serialize(Timestamp timestamp, JsonGenerator gen, SerializerProvider provider) throws java.io.IOException {
                if (timestamp != null) {
                    gen.writeString(dateFormat.format(new Date(timestamp.getTime())));
                } else {
                    gen.writeNull();
                }
            }
        });
        timestampModule.addDeserializer(Timestamp.class, new StdDeserializer<Timestamp>(Timestamp.class) {
            @Override
            public Timestamp deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
                String dateStr = p.getText();
                try {
                    Date date = dateFormat.parse(dateStr);
                    return new Timestamp(date.getTime());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse Timestamp: " + dateStr, e);
                }
            }
        });
        
        objectMapper.registerModule(javaTimeModule);
        objectMapper.registerModule(timestampModule);
        
        return objectMapper;
    }
}
