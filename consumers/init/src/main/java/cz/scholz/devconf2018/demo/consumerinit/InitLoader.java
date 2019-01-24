package cz.scholz.devconf2018.demo.consumerinit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class InitLoader {
    public static void main(String[] args) throws InterruptedException, IOException, ParseException {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("BOOTSTRAP_SERVERS"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        String topic = System.getenv("TOPIC");

        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);

        while (true) {
            JSONParser parser = new JSONParser();
            JSONArray initData = (JSONArray) parser.parse(new FileReader("/init.json"));
            Iterator<JSONObject> iter = initData.iterator();

            while (iter.hasNext()) {
                JSONObject payload = iter.next();
                String symbol = (String) payload.get("symbol");

                ProducerRecord record = new ProducerRecord<String, String>(topic, symbol, payload.toJSONString());
                Future<RecordMetadata> futureResult = producer.send(record);

                try {
                    RecordMetadata result = futureResult.get();

                    System.out.println("-I- message sent:" +
                            "\n\t Topic: " + record.topic() +
                            "\n\t Partition: " + record.partition() +
                            "\n\t Offset: " + result.offset() +
                            "\n\t Key: " + record.key() +
                            "\n\t Value: " + record.value());

                    Thread.sleep(1000);
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("-E- Failed to send a message: " + e);
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}