package io.ticofab.legacyprocessor2;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import reactor.core.publisher.Flux;

import java.util.Properties;
import java.util.Random;

public class LegacyProcessor2 {

    public static void main(String[] args) {
        log("starting legacy processor 2");
        KafkaProducer<String, String> kafkaProducer = getKafkaProducer();

        var acceptorRS = SocketAcceptor.forRequestStream(
                payload -> {
                    String data = payload.getDataUtf8();
                    log("received data in stream: " + data);

                    // simulate some work
                    keepBusy();

                    // publish to destination topic
                    kafkaProducer.send(new ProducerRecord<String, String>("SecondTopic", data));

                    return Flux.empty();
                }
        );

        var server = RSocketServer
                .create(acceptorRS)
                .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .bind(TcpServerTransport.create("localhost", 7000))
                .subscribe(
                        c -> log("connection established"),
                        e -> log("error: " + e),
                        () -> log("server creation completed.")
                );

        log("started server");

        // make the program wait for us before running to completion.
        try {
            Thread.sleep(100000);
            log("Timer expired. I'm outta here!");
            server.dispose();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static KafkaProducer<String, String> getKafkaProducer() {
        // Set up client Java properties
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        props.setProperty(ProducerConfig.ACKS_CONFIG, "1");
        return new KafkaProducer<>(props);
    }

    private static final Random random = new Random();

    /**
     * Keeps the machine busy for a random number of seconds between 1 and 3
     */
    private static void keepBusy() {
        var seconds = random.nextInt(3) + 1;
        log("keeping busy for " + seconds + " seconds.");
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void log(String s) {
        System.out.println(s);
    }
}
