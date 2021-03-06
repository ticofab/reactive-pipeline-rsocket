package io.ticofab.legacyprocessor1

import akka.stream._
import akka.stream.stage._
import io.rsocket._
import io.rsocket.core._
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.util._
import reactor.util.retry.Retry

import java.time.Duration

class RSocketSink(port: Int, host: String = "0.0.0.0") extends GraphStage[SinkShape[String]] {

  val in: Inlet[String] = Inlet("RSocketSinkInput")
  var socket: RSocket   = null // don't use null in prod!

  override def shape: SinkShape[String] = SinkShape(in)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      override def preStart(): Unit = {
        println(s"pre start, connecting to $host:$port")
        socket = RSocketConnector
          .create()
          .reconnect(Retry.backoff(50, Duration.ofMillis(500)))
          .connect(TcpClientTransport.create("localhost", 7000))
          .block
        println("socket connected")

        // Akka Streams semantics: ask an element as soon as you start.
        //   'pull' tells upstream that we have capacity to handle the next item.
        pull(in)
      }

      setHandler(
        in,
        new InHandler {
          override def onPush(): Unit = {

            // Akka Streams semantics: 'grab' returns the last item that has
            //   been pulled (see above) so that we can do something with it.
            val payload = grab(in)

            // do operation
            socket
              .requestStream(DefaultPayload.create(payload))
              .doOnRequest(_ => pull(in))
              .doOnError(e => println(s"error ${e.getCause}"))
              .onErrorStop()
              .blockFirst()
          }

          override def onUpstreamFinish(): Unit = {
            println("RSocketSink onUpstreamFinish")
            super.onUpstreamFinish()
            socket.dispose()
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            println("RSocketSink, onUpstreamFailure")
            super.onUpstreamFailure(ex)
            socket.dispose()
          }
        }
      )
    }
}
