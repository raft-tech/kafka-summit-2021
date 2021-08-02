package tech.goraft.kafka.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Map;
import java.util.Properties;

@Slf4j
public class KafkaStreamsDemo {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    //Give the Streams application a unique name. Must be unique in the Kafka cluster.
    private static final String APPLICATION_ID = System.getenv().getOrDefault("APPLICATION_ID", "kafka-streams-filter");
    private static final String BOOTSTRAP_SERVERS = System.getenv().getOrDefault("BOOTSTRAP_SERVERS", null);
    private static final String KAFKA_TOPIC = System.getenv().getOrDefault("KAFKA_TOPIC", null);
    private static final boolean KAFKA_SASL_ENABLED =  Boolean.parseBoolean(System.getenv().getOrDefault("KAFKA_SASL_ENABLED", "true"));
    private static final String KAFKA_SASL_USERNAME = System.getenv().getOrDefault("SASL_USERNAME", null);
    private static final String KAFKA_SASL_PASSWORD = System.getenv().getOrDefault("SASL_PASSWORD", null);

    public static void main(String[] args) {
        final Properties streamsConfiguration = createStreamConfiguration();
        final StreamsBuilder builder = new StreamsBuilder();
        createMessageFilterStream(builder);
        final KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);

        streams.cleanUp();

        streams.setUncaughtExceptionHandler((thread, throwable) -> log.error("Uncaught error for Kafka Streams, thread: {}", thread, throwable));

        // Begin consuming messages
        log.info("Starting Kafka Streams");
        streams.start();

        // Add shutdown hook to gracefully close the Streams application
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static Properties createStreamConfiguration() {
        log.info("Configuring Kafka Stream properties");
        final Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        streamsConfiguration.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class);
        if(KAFKA_SASL_ENABLED) {
            streamsConfiguration.put(StreamsConfig.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_PLAINTEXT.name);
            String saslProp = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";
            String saslConfig = String.format(saslProp, KAFKA_SASL_USERNAME, KAFKA_SASL_PASSWORD);
            streamsConfiguration.put("sasl.mechanism", "PLAIN");
            streamsConfiguration.put("sasl.jaas.config", saslConfig);
        }

        return streamsConfiguration;
    }

    private static String getFullNameFromKafkaMessage(Map message) {
        return message.get("first_name") + (String) message.get("last_name");
    }

    private static void createMessageFilterStream(final StreamsBuilder builder) {
        log.info("Setting up Kafka Streams for topic: {}", KAFKA_TOPIC);

        builder.stream(KAFKA_TOPIC, Consumed.with(Serdes.String(), new JsonSerde<>(Map.class)))
                .filter((name, message) -> KAFKA_SASL_USERNAME.equals(getFullNameFromKafkaMessage(message)))
                .foreach((key, value) -> {
                    try {
                        log.debug("Message: {}", objectMapper.writeValueAsString(value));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });
    }
}
