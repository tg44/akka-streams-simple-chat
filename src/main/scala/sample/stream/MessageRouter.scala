package sample.stream

import akka.actor.{Actor, ActorLogging, ActorRef}
import sample.stream.MessageRouter.{ConnectionClosed, SignUp}

object MessageRouter {
  case class SignUp(uid: String)
  case class ConnectionClosed(uid: String)
}

class MessageRouter extends Actor with ActorLogging{

  var users = Map.empty[String, ActorRef]


  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    log.debug(self.path toString)
  }

  override def receive = {
    case SignUp(uid) => users += (uid -> sender())
    case x: Message => users(x.uid) ! x
    case ConnectionClosed(uid) => users -= uid
  }
}
