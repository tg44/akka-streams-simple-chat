package gatling.protocol.protoclient

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gatling.protocol.protoclient.MessageReceiver.{ReceivedMsg, ReceivedUserList}
import io.gatling.commons.stats.OK
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings


object MessageRouter {
	def props(statsEngine: StatsEngine, session: Session, name: String): Props = Props(new MessageRouter(statsEngine,session,name))
}

class MessageRouter(statsEngine: StatsEngine, session: Session, name: String) extends Actor with ActorLogging{

	var senderActor : ActorRef = null
	var userThread: ActorRef = null

	@throws[Exception](classOf[Exception])
	override def preStart(): Unit = {
		super.preStart()
		log.debug(self.path toString)
	}

	override def receive = {
		case x:ActorRef => senderActor = x
		case x: String => {
			userThread = sender
			senderActor ! x
		}
		case ReceivedMsg(x) => {
			if(x.contains(">")) {
				val timings = ResponseTimings(x.split(">")(0).toLong, System.currentTimeMillis)
				statsEngine.logResponse(session, name, timings, OK, None, None)
			}
		}
		case ReceivedUserList(x) => {
			log.debug(x)
			userThread ! x
		}
	}
}
