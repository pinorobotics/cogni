/*
 * Copyright 2026 pinorobotics
 * 
 * Website: https://github.com/pinorobotics
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pinorobotics.cogni;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.internal.Json.JsonCodec;
import dev.langchain4j.spi.json.JsonCodecFactory;
import java.lang.reflect.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author aeon_flux aeon_flux@eclipso.ch
 */
public class CogniJsonCodecFactory implements JsonCodecFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CogniJsonCodecFactory.class);

    private static final ObjectMapper mapper =
            new ObjectMapper()
                    // serialize Instant as date time string and not timestamp
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule())
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    // fail in case of hallucinated fields
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @Override
    public JsonCodec create() {
        LOGGER.info("Create JSON codec");
        return new JsonCodec() {

            @Override
            public String toJson(Object o) {
                try {
                    var json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
                    return json;
                } catch (Exception e) {
                    LOGGER.error("JSON serialization error: {}", e.getMessage());
                    LOGGER.debug(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T fromJson(String json, Class<T> type) {
                try {
                    return mapper.readValue(json, type);
                } catch (Exception e) {
                    LOGGER.error("JSON deserialization error: {}", e.getMessage());
                    LOGGER.debug(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }

            @Override
            public <T> T fromJson(String json, Type type) {
                try {
                    return mapper.readValue(json, mapper.constructType(type));
                } catch (Exception e) {
                    LOGGER.error("JSON deserialization error: {}", e.getMessage());
                    LOGGER.debug(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
