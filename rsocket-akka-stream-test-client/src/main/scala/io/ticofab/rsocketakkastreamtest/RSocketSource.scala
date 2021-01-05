package io.ticofab.rsocketakkastreamtest

import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import io.rsocket.core.RSocketServer
import io.rsocket.frame.decoder.PayloadDecoder
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.{Payload, SocketAcceptor}
import reactor.core.publisher.Mono

import java.util.concurrent.LinkedBlockingDeque

/**
 * Implements a Source that will listen to incoming RSocket fire N forget connections and
 * will push the received elements downstream.
 * @param port The TCP port to open for incoming connections
 * @param host The host (defaults to localhost)
 */
class RSocketSource(port: Int, host: String = "0.0.0.0")
  extends GraphStage[SourceShape[Array[Byte]]] {

  // the outlet port of this stage which produces Ints
  val out: akka.stream.Outlet[Array[Byte]] = Outlet("RsocketSourceOut")

  // Coordination queue
  // TODO: use another way to only ask for items if there is room?
  //  or maybe delegate to something else downstream
  private val blockingQueue = new LinkedBlockingDeque[Array[Byte]]()

  override val shape: SourceShape[Array[Byte]] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      val fNFSocketAcceptor: SocketAcceptor =
        SocketAcceptor.forFireAndForget((payload: Payload) => {
          // Get data
          val buffer = payload.getData
          val data = new Array[Byte](buffer.remaining())
          buffer.get(data)
          println(s"received data: ${payload.getDataUtf8}")
          // Queue it for processing
          blockingQueue.add(data)
          payload.release()
          Mono.empty()
        })

      // Pre start. create server
      override def preStart(): Unit = {
        RSocketServer
          .create(fNFSocketAcceptor)
          .payloadDecoder(PayloadDecoder.ZERO_COPY)
          .bind(TcpServerTransport.create(host, port))
          .doOnSubscribe(s => println(s"subscription: $s"))
          .subscribe

        println(s"Bound RSocket server to $host:$port")
      }

      // create a handler for the outlet
      setHandler(
        out,
        new OutHandler {
          // when you are asked for data (pulled for data)
          override def onPull(): Unit = {
            // emit an element on the outlet, if exists
            push(out, blockingQueue.take())
          }
        }
      )
    }
}
