package io.ticofab.sourcesproducer

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import java.time.{LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.DurationInt


object SourcesProducer extends App {
  implicit val system: ActorSystem = ActorSystem("SourcesProducer")
  val config = system.settings.config.getConfig("akka.kafka.producer")
  val producerSettings = ProducerSettings(config, new StringSerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")

  val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

  Source
    .tick(1.second, 1.second, ())
    .map(_ => LocalTime.now.format(formatter))
    .map(now => {
      val msg = "Message sent at " + now
      println(msg)
      new ProducerRecord[String, String]("FirstTopic", msg)
    })
    .runWith(Producer.plainSink(producerSettings))

}
