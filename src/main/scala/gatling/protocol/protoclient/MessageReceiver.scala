package gatling.protocol.protoclient

import akka.actor.{ActorRef, Props}
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ActorSubscriber, MaxInFlightRequestStrategy}
import akka.util.ByteString
import gatling.protocol.protoclient.MessageReceiver.{ReceivedMsg, ReceivedUserList}

object MessageReceiver {
	def props(router: ActorRef): Props = Props(new MessageReceiver(router))

	case class ReceivedMsg(payLoad: String)

	case class ReceivedUserList(payLoad: String)

}

class MessageReceiver(router: ActorRef) extends ActorSubscriber {
	val MaxBufferSize = 100

	override val requestStrategy = new MaxInFlightRequestStrategy(max = MaxBufferSize) {
		override def inFlightInternally: Int = 0 //not safe :)
	}

	def receive = {
		case OnNext(x: ByteString) => {
			val y = x.utf8String
			if(y.contains(">")) {
				router ! ReceivedMsg(y)
			}
			else if(y.contains(";")) {
				router ! ReceivedUserList(y)
			}
		}
	}
}
