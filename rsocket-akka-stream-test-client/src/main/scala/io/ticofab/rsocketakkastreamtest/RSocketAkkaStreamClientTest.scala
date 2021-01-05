package io.ticofab.rsocketakkastreamtest

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import io.rsocket.core.{RSocketClient, RSocketConnector}
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.util.DefaultPayload
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

import java.nio.charset.StandardCharsets
import java.time.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object RSocketAkkaStreamClientTest extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("legacyProcessor1")

  val source = RSocketConnector.create
    .reconnect(Retry.backoff(50, Duration.ofMillis(500)))
    .connect(TcpClientTransport.create("localhost", 7000))

  // establishes 10 fireAndForget requests
  // fireAndForgetConnection

  // initiates a request/response connection
  // requestResponseConnection

  // asks for a stream of stuff
  acceptFireAndForgetConnections
    .onComplete{
      case _ => println("Successfully completed")
      case ex => println(s"Completed with error $ex")
    }

  def requestResponseConnection = {
    RSocketClient
      .from(source)
      .requestResponse(Mono.just(DefaultPayload.create("Test Request")))
      .doOnSubscribe(_ => println("Executing Request"))
      .doOnNext(d => {
        println(s"Received response data ${d.getDataUtf8}")
        d.release
      })
      .repeat(10)
      .blockLast
  }

  def fireAndForgetConnection = {
    RSocketClient
      .from(source)
      .fireAndForget(Mono.just(DefaultPayload.create("Fire And Forget msg")))
      .doOnSubscribe(_ => println("Executing Request"))
      .repeat(10)
      .blockLast
  }

  def acceptFireAndForgetConnections = {
      Source
        .fromGraph(new RSocketSource(7000))
        .map(bytes => new String(bytes, StandardCharsets.UTF_8))
        .runForeach(s => println(s"got a string! $s"))
  }
}
