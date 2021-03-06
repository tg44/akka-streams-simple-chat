package server

import akka.NotUsed
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

		val msgGraph = GraphMaker.recieverAktors
		val bidi = GraphMaker.bidiWithCallback

		val flow = bidi.join(msgGraph)

		val msgHandler = Flow[ByteString].via(flow)

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

object GraphMaker {
	implicit val sys = ActorSystem("ClientAndServer")
	val router = sys.actorOf(Props(new MessageRouter()), "router")

	def recieverAktors(implicit sys: ActorSystem) = GraphDSL.create() { implicit b =>
		val messageManagerSource: SourceShape[InMsg] = b.add(Source.actorPublisher[InMsg](MessageSender.props(router)))
		val messageManagerSink: SinkShape[InMsg] = b.add(Sink.actorSubscriber(MessageReceiver.props(router)))

		FlowShape(messageManagerSink.in, messageManagerSource.out)
	}

	def escape(raw: String): String = {
		import scala.reflect.runtime.universe._
		Literal(Constant(raw)).toString
	}

	def bsframer: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].via(Framing.delimiter(ByteString("!"), maximumFrameLength = 1000, allowTruncation = true))

	def printer: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].map(x => {
		println(escape(x.utf8String)); x
	})

	def bidiWithCallback = BidiFlow.fromGraph(GraphDSL.create() { implicit b =>
		import GraphDSL.Implicits._
		def toInMsg(x: ByteString): InMsg = {
			val y = x.utf8String
			val msg = y.split(":")
			if(msg.size == 2) {
				Message(msg(1), msg(0))
			}
			else if(y.trim.equals("list-u")) {
				ListUsers()
			}
			else if(y.trim.equals("dc")) {
				Disconnect()
			} else {
				println("bad msg: " + y)
				new InMsg()
			}
		}

		val merger = b.add(Merge[InMsg](2))
		val bcast = b.add(Broadcast[InMsg](2))
		val framer = b.add(bsframer)
		//var printer2 = b.add(printer)

		val convertFrom = b.add(Flow[ByteString].map(toInMsg))
		val convertTo = b.add(Flow[InMsg]
			.filter(msg => msg.isInstanceOf[Message])
			.map(msg => msg.asInstanceOf[Message])
			.map(msg => ByteString(msg.payload + "!")))

		val filterControl = b.add(Flow[InMsg].filter(
			k => k.isInstanceOf[InControl]
		))

		framer ~> convertFrom ~> merger.in(0)
		bcast.out(1) ~> filterControl ~> merger.in(1)
		bcast.out(0) ~> convertTo

		BidiShape(framer.in, merger.out, bcast.in, convertTo.out)
	})
}

