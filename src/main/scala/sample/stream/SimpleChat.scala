package sample.stream

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.util.{Failure, Success}

object SimpleChat {
  def main(args: Array[String]): Unit = {
      val system = ActorSystem("ClientAndServer")
      val (address, port) = ("127.0.0.1", 6000)
      server(system, address, port)
  }

  def uuid() = java.util.UUID.randomUUID.toString

  def server(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys: ActorSystem = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val router = sys.actorOf(Props(new MessageRouter()),"router")

    val msgGraph = GraphDSL.create() { implicit b =>
      val uid = uuid() // this evaluated exactly once :(
      val source: Source[Message, ActorRef] = Source.actorPublisher[Message](MessageSender.props(router))
      val messageManagerSource: SourceShape[ByteString] = b.add(source.map(x=>ByteString(x.payload)))
      val messageManagerSink: SinkShape[ByteString] = b.add(Sink.actorSubscriber(MessageReciever.props(router)))

      FlowShape(messageManagerSink.in, messageManagerSource.out)
    }

    val msgHandler = Flow[ByteString].via(msgGraph)

    val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      println("Client connected from: " + conn.remoteAddress)
      conn handleWith msgHandler
    }

    val connections = Tcp().bind(address, port)
    val binding = connections.to(handler).run()

    binding.onComplete {
      case Success(b) =>
        println("Server started, listening on: " + b.localAddress)
      case Failure(e) =>
        println(s"Server could not bind to $address:$port: ${e.getMessage}")
        system.shutdown()
    }
  }
}

final case class Message(payload: String, uid: String)