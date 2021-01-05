package io.ticofab.legacyprocessor1

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer

import java.nio.charset.StandardCharsets

object LegacyProcessor1App extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("legacyProcessor1")

  val config           = actorSystem.settings.config.getConfig("our-kafka-consumer")
  val consumerSettings = ConsumerSettings(config, new StringDeserializer, new StringDeserializer)

  println("Legacy Processor 1 starting.")

  // 1.
  // starts the RSocket example from the official examples
  // ExampleRSocket.start

  // 2.
  // listens to incoming RSocket connections, replies nothing and pushes the data forward
  // Source
  //   .fromGraph(new RSocketSource(7000))
  //   .map(bytes => new String(bytes, StandardCharsets.UTF_8))
  //   .runForeach(println)

  // 3.
  // connects to an active RSocket server and fires elements to it
//    Source(List("string1", "string2", "string3"))
//      .map(_.getBytes)
//      .to(Sink.fromGraph(new RSocketSink(7000)))
//      .run()

  // 4.
  // connects to a running kafka topics and consumes from there
  kafkaSource
//    .to(Sink.foreach(cr => println(s"read kafka message with value ${new String(cr, StandardCharsets.UTF_8)}")))
    .to(Sink.fromGraph(new RSocketSink(7000)))
    .run()

  def stringSource = Source(List("string1", "string2", "string3")).map(_.getBytes)

  def kafkaSource =
    Consumer
      .plainSource(
        consumerSettings,
        Subscriptions.assignmentWithOffset(new TopicPartition("FirstTopic", 0), 0)
      )
      .map(msg => {
        println(s"read message from kafka: ${msg.value} at offset ${msg.offset}")
        msg.value.getBytes
      })
}
