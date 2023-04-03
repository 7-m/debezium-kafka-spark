package dev.mufaddal.report;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.streaming.StreamingQueryException;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.decode;

public class App {


    /**
     * Env variables to configure applicaiton
     */
    public static final String KAFKA_BROKER_URI;
    public static final String SPARK_TOPIC;

    static {
        KAFKA_BROKER_URI = getEnvOrThrow("KAFKA_BROKERS_URI");
        SPARK_TOPIC = getEnvOrThrow("SPARK_TOPIC");
    }

    private static String getEnvOrThrow(String env) {
        String val = System.getenv(env);
        if (val == null) {
            throw new RuntimeException(String.format("env variable %s is not defined", env));
        }
        return val;
    }

    public static void main(String[] args) throws TimeoutException, StreamingQueryException, InterruptedException {
        SparkSession ss = SparkSession.builder().appName("Kafkfa Mysql").master("local[2]").getOrCreate();

        var stream = ss.readStream()
                       .format("kafka")
                       .option("kafka.bootstrap.servers", KAFKA_BROKER_URI)
                       .option("subscribe", SPARK_TOPIC)
                       .option("startingOffsets", "earliest")
                       .load();
        ss.sparkContext().setLogLevel("warn");

        // fetch the schema form an example json
        var schema = ss.read().option("multiline", true).json("data.json").schema();


        // project the json as columns
        stream = stream.select(from_json(decode(col("value"), "UTF-8"), schema).as("parsed"));
        stream = stream.select("parsed.*");

        // sum on the price column based on a 5 minute tumbling window
        stream = stream
                .groupBy(
                        window(col("timestamp"), "5 minutes")
                )
                .agg(Map.of("price", "sum"));


        stream.writeStream()
              .format("console")
              .outputMode("update")
              .option("truncate", false)
              .start()
              .awaitTermination();


    }


}
