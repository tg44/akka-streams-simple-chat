package gatling.protocol.protoclient

import akka.actor._
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ActorPublisher, ActorSubscriber, MaxInFlightRequestStrategy}
import akka.util.ByteString
import gatling.protocol.protoclient.MessageReceiver.{ReceivedMsg, ReceivedUserList}
import io.gatling.commons.stats.OK
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings

import scala.annotation.tailrec



object MessageSender {
  def props(router: ActorRef): Props = Props(new MessageSender(router))
}

class MessageSender(router: ActorRef) extends ActorPublisher[String] {
  val MaxBufferSize = 100
  var buf = Vector.empty[String]


  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    context.watch(router)
    router ! self
  }

  def receive = {
    case msg: String if buf.size == MaxBufferSize =>
    case msg: String =>
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
    case Terminated(ref) =>
      onComplete()
  }

  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
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

