package io.github.yienruuuuu.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;

/**
 * Provides the shared JSON mapper for the application.
 *
 * <p>Business classes should inject this bean instead of creating their own
 * {@link ObjectMapper} instances.</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(
            Jackson2ObjectMapperBuilder builder,
            List<Jackson2ObjectMapperBuilderCustomizer> customizers
    ) {
        customizers.forEach(customizer -> customizer.customize(builder));
        return builder.createXmlMapper(false).build();
    }
}
