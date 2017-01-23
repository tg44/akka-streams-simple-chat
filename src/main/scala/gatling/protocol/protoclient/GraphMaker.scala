package gatling.protocol.protoclient

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Flow, Framing, GraphDSL, RunnableGraph, Sink, Source, Tcp}
import akka.util.ByteString


object GraphMaker {

	import GraphDSL.Implicits._

	implicit val sys = ActorSystem("mySystem")

	def escape(raw: String): String = {
		import scala.reflect.runtime.universe._
		Literal(Constant(raw)).toString
	}

	def bsframer: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].via(Framing.delimiter(ByteString("˙"), maximumFrameLength = 1000, allowTruncation = true))

	def printer: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].map(x => {
		println(escape(x.utf8String));
		x
	})

	def tcpFlow(ip: String, port: Int, router: ActorRef) = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
		val source = b.add(Source.actorPublisher[String](MessageSender.props(router)))
		val tcp = b.add(Tcp().outgoingConnection(ip, port))
		val sink = b.add(Sink.actorSubscriber(MessageReceiver.props(router)))
		val mapToByteStr = b.add(Flow[String].map(x => ByteString(x)))
		//val framer: FlowShape[ByteString, ByteString] = b.add(bsframer)
		//val separator = b.add(Flow[String].map(x => ByteString(x + "˙")))

		source ~> mapToByteStr ~> tcp ~> sink
		ClosedShape
	}
	)
}
