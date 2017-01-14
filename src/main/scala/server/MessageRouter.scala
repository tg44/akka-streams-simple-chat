package server

import akka.actor.{Actor, ActorLogging, ActorRef}

object MessageRouter {
}

class MessageRouter extends Actor with ActorLogging{

  var users = Map.empty[String, ActorRef]

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    log.debug(self.path toString)
  }

  override def receive = {
    case CtrlSignUp(uid) => users += (uid -> sender())
    case x: Message => users(x.uid) ! x
    case CtrlListUsers(uid) => users(uid) ! Message(users.keys.fold("")((a,u) => a + u + ";"),uid)
    case CtrlDisconnect(uid) => users -= uid
  }
}
