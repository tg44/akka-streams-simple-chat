package sample.stream

import akka.actor.{ActorRef, Props}
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.actor.{ActorSubscriberMessage, ActorPublisher}

import scala.annotation.tailrec


object MessageSender {
  def uuid() = java.util.UUID.randomUUID.toString
  def props(router: ActorRef): Props = Props(new MessageSender(uuid(), router))
  def props(uid: String, router: ActorRef): Props = Props(new MessageSender(uid, router))
  case object MsgAccepted
  case object MsgDenied
}

class MessageSender(uid: String, router: ActorRef) extends ActorPublisher[Message] {
  import MessageSender._
  import ActorSubscriberMessage._

  val MaxBufferSize = 100
  var buf = Vector.empty[Message]

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    router ! MessageRouter.SignUp(uid)
    buf :+= Message(uid,uid)
  }

  def receive = {
    case msg: Message if buf.size == MaxBufferSize =>
      sender() ! MsgDenied
    case msg: Message =>
      sender() ! MsgAccepted
      if (buf.isEmpty && totalDemand > 0)
        onNext(msg)
      else {
        buf :+= msg
        deliverBuf()
      }
    case Request(_) =>
      deliverBuf()
    case Cancel =>
      context.stop(self)
  }

  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      /*
       * totalDemand is a Long and could be larger than
       * what buf.splitAt can accept
       */
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }
}

