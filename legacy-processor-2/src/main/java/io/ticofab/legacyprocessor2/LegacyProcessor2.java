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

        // define the behaviour upon receiving data from the RSocket source
        var acceptorRS = SocketAcceptor.forRequestStream(
                payload -> {
                    String data = payload.getDataUtf8();

                    // encapsulates some legacy processing logic
                    String processedData = doSomeProcessing(data);

                    // publish to destination topic
                    kafkaProducer.send(new ProducerRecord<String, String>("SecondTopic", processedData));

                    // when we return this, we signal that we are ready to process
                    // the next item, conforming to the Reactive Streams standard.
                    return Flux.empty();
                }
        );

        // create a RSocket server to accept items from upstream.
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
            Thread.sleep(1000000);
            log("Timer expired. I'm outta here!");
            server.dispose();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Consfigures and instantiates a KafkaProducer object.
     *
     * @return The KafkaProducer object.
     */
    private static KafkaProducer<String, String> getKafkaProducer() {
        // Set up client Java properties
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.ACKS_CONFIG, "1");
        return new KafkaProducer<>(props);
    }

    private static final Random random = new Random();

    /**
     * Keeps the machine busy for a random number of seconds between 1 and 3
     */
    private static String doSomeProcessing(String data) {
        try {
            var seconds = random.nextInt(4) + 1;
            Thread.sleep(seconds * 1000);
            log("received " + data + " and processed for " + seconds + " seconds.");
            return data + "-twice";
        } catch (InterruptedException e) {
            // something went wrong while processing, signal it downstream
            return data + "-then-failed";
        }
    }

    // a simple shortcut println method
    private static void log(String s) {
        System.out.println(s);
    }
}
