package sample.stream

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
  var buf = Vector.empty[Message]

  override val requestStrategy = new MaxInFlightRequestStrategy(max = MaxBufferSize) {
    override def inFlightInternally: Int = buf.size
  }

  def receive = {
    case OnNext(x:ByteString) =>
      val msg = x.utf8String.split(":")
      if(msg.size == 2) {
        router ! Message(msg(1), msg(0))
      }
    case x:Message =>
      buf = buf :+ x
  }
}