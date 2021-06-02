package io.ticofab.legacyprocessor1

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer

object LegacyProcessor1App extends App {

  println("Legacy Processor 1 starting.")
  implicit val actorSystem: ActorSystem = ActorSystem("legacyProcessor1")

  // settings to consume from a kafka topic
  val config            = actorSystem.settings.config.getConfig("our-kafka-consumer")
  val consumerSettings  = ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
  val kafkaSubscription = Subscriptions.assignmentWithOffset(new TopicPartition("FirstTopic", 0), 0)

  // the RSocket sink that will propagate items downstream
  val rSocketSink = Sink.fromGraph(new RSocketSink(7000))

  // connects to a running kafka topics and consumes from there
  Consumer
    .plainSource(consumerSettings, kafkaSubscription)
    .map(processItem)
    .map(_.getBytes)
    .to(rSocketSink)
    .run()

  // simulates processing of an item received from the topic
  def processItem(msg: ConsumerRecord[String, String]) = {
    println(s"read from kafka: '${msg.value}' at offset ${msg.offset}")
    msg.value
  }
}
