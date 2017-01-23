package server

import akka.actor.{Actor, ActorLogging, ActorRef}

object MessageRouter {
}

class MessageRouter extends Actor with ActorLogging{

  var users = Map.empty[String, ActorRef]

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
  }

  override def receive = {
    case CtrlSignUp(uid) => users += (uid -> sender())
    case x: Message => {
			if(users.contains(x.uid))
				users(x.uid) ! x
			else
				println("key not found: "+x.uid)
		}
    case CtrlListUsers(uid) => users(uid) ! Message(users.keys.fold("")((a,u) => a + u + ";")+"!",uid)
    case CtrlDisconnect(uid) => users -= uid
  }
}
