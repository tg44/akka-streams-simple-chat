package gatling.protocol

import akka.actor._
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{MaxInFlightRequestStrategy, ActorSubscriber, ActorPublisher}
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.util.ByteString
import gatling.protocol.MessageReciever.{RecievedUserList, RecievedMsg}
import io.gatling.commons.stats.OK
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings

import scala.annotation.tailrec


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
    case RecievedMsg(x) => {
      if(x.contains(">")) {
        val timings = ResponseTimings(x.split(">")(0).toLong, System.currentTimeMillis)
        statsEngine.logResponse(session, name, timings, OK, None, None)
      }
    }
    case RecievedUserList(x) => {
      log.debug(x)
      userThread ! x
    }
  }
}

object MessageSender {
  def props(router: ActorRef): Props = Props(new MessageSender(router))
}

class MessageSender(router: ActorRef) extends ActorPublisher[ByteString] {
  val MaxBufferSize = 100
  var buf = Vector.empty[ByteString]


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
        onNext(ByteString(msg))
      else {
        buf :+= ByteString(msg)
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
object MessageReciever {
  def props(router: ActorRef): Props = Props(new MessageReciever(router))
  case class RecievedMsg(payLoad : String)
  case class RecievedUserList(payLoad : String)
}

class MessageReciever(router: ActorRef) extends ActorSubscriber {
  val MaxBufferSize = 100

  override val requestStrategy = new MaxInFlightRequestStrategy(max = MaxBufferSize) {
    override def inFlightInternally: Int = 0 //not safe :)
  }

  def receive = {
    case OnNext(x:ByteString) => {
      val y = x.utf8String
      if(y.contains(">")){
        router ! RecievedMsg(y)
      }
      else if (y.contains(";")){
        router ! RecievedUserList(y)
      }
    }
  }
}
