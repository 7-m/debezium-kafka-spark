package dev.mufaddal.kstreamfilter;

import com.google.gson.Gson;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import dev.mufaddal.kstreamfilter.model.DebeziumRecord;
import dev.mufaddal.kstreamfilter.model.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class App {



  /** Env variables to configure applicaiton */
    public static final String KAFKA_BROKER_URI;
    public static final String APP_ID;
    public static final String DBZ_TOPIC;
    public static final String SPARK_TOPIC;

    static {
        KAFKA_BROKER_URI = getEnvOrThrow("KAFKA_BROKERS_URI");
        APP_ID = getEnvOrThrow("APP_ID");
        DBZ_TOPIC = getEnvOrThrow("DBZ_TOPIC");
        SPARK_TOPIC = getEnvOrThrow("SPARK_TOPIC");
    }

    private static String getEnvOrThrow(String env) {
        String val = System.getenv(env);
        if (val == null) {
            throw new RuntimeException(String.format("env variable %s is not defined", env));
        }
        return val;
    }

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    static void runKafkaStreams(final KafkaStreams streams) {
        final CountDownLatch latch = new CountDownLatch(1);
        streams.setStateListener((newState, oldState) -> {
            if (oldState == KafkaStreams.State.RUNNING && newState != KafkaStreams.State.RUNNING) {
                latch.countDown();
            }
        });

        streams.start();

        try {
            latch.await();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        logger.info("Streams Closed");
    }


    private static Topology buildTopology(String DBZ_TOPIC, String SPARK_TOPIC) {
        StreamsBuilder builder = new StreamsBuilder();
        builder.stream(DBZ_TOPIC, Consumed.with(exampleSerde(), exampleSerde()))
               .peek((k, v) -> System.out.printf("input %s and value %s ----", k, v))
               .map((key, value) -> new KeyValue<>(value.getPayload().getId(), value.getPayload()))
               .peek((k, v) -> System.out.printf("output %s and value %s\n", k, v))
               .to(SPARK_TOPIC, Produced.with(Serdes.Integer(), payloadSerde()));
        return builder.build();

    }

    public static void main(String[] args) throws Exception {


        Properties props = new Properties();
        props.setProperty("bootstrap.servers", KAFKA_BROKER_URI);
        props.setProperty("application.id", APP_ID);
        props.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");
        props.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, "org.apache.kafka.common.serialization.Serdes$StringSerde");


        KafkaStreams kafkaStreams = new KafkaStreams(
                buildTopology(DBZ_TOPIC, SPARK_TOPIC),
                props);

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));

        logger.info("Kafka Streams 101 App Started");
        runKafkaStreams(kafkaStreams);

    }


    static Serde<DebeziumRecord> exampleSerde() {
        Serializer<DebeziumRecord> o = (topic, data) -> new Gson().toJson(data).getBytes(StandardCharsets.UTF_8);

        Deserializer<DebeziumRecord> d = (topic, data) -> new Gson().fromJson(new String(data), DebeziumRecord.class);

        return Serdes.serdeFrom(o, d);
    }

    static Serde<Payload> payloadSerde() {
        Serializer<Payload> o = (topic, data) -> new Gson().toJson(data).getBytes(StandardCharsets.UTF_8);

        Deserializer<Payload> d = (topic, data) -> new Gson().fromJson(new String(data), Payload.class);

        return Serdes.serdeFrom(o, d);
    }

}
