package server

import akka.actor.{ActorRef, Props}
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{MaxInFlightRequestStrategy, ActorSubscriber}
import akka.util.ByteString

object MessageReciever {
  def props(router: ActorRef): Props = Props(new MessageReciever(router))
}

class MessageReciever(router: ActorRef) extends ActorSubscriber {
  import akka.stream.actor.ActorPublisherMessage._
  import MessageReciever._

  val MaxBufferSize = 100
  var uid = ""

  override val requestStrategy = new MaxInFlightRequestStrategy(max = MaxBufferSize) {
    override def inFlightInternally: Int = 0 //not safe :)
  }

  def receive = {
    case OnNext(MyUid(inUid)) =>
      uid = inUid
    case OnNext(x:Message) =>
      router ! x
    case OnNext(x:Disconnect) =>
      router ! CtrlDisconnect(uid)
    case OnNext(x:ListUsers) =>
      router ! CtrlListUsers(uid)
  }
}