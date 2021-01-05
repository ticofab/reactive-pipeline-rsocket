package io.ticofab.legacyprocessor1

import io.rsocket.core.RSocketServer
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.util.DefaultPayload
import io.rsocket.{Payload, SocketAcceptor}
import reactor.core.Disposable
import reactor.core.publisher.Mono

import java.time.Duration

object ExampleRSocket {
  private val acceptor = SocketAcceptor.forRequestResponse((p: Payload) => {
    val data = p.getDataUtf8
    println(s"Received request data $data")
    val responsePayload = DefaultPayload.create("Echo: " + data)
    p.release
    Mono.just(responsePayload)
  })

  def start: Disposable = {
    println("Starting example RSocketServer")
    RSocketServer
      .create(acceptor)
      .bind(TcpServerTransport.create("localhost", 7000))
      .delaySubscription(Duration.ofSeconds(5))
      .doOnNext(cc => println(s"Server started on the address : ${cc.address}"))
      .subscribe
  }
}
