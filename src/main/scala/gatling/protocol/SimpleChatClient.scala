package gatling.protocol

import akka.NotUsed
import akka.actor._
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{MaxInFlightRequestStrategy, ActorSubscriber, ActorSubscriberMessage, ActorPublisher}
import akka.stream.{Graph, ActorMaterializer, ClosedShape, SinkShape}
import akka.stream.scaladsl._
import akka.util.{Timeout, ByteString}
import gatling.protocol.MessageReciever.{RecievedUserList, RecievedMsg}
import io.gatling.commons.stats.OK
import io.gatling.core.action.{ExitableAction, Action}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.protocol.{ProtocolComponents, ProtocolComponentsRegistry}
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import akka.pattern.ask

import scala.annotation.tailrec
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.Random

class SimpleChatConnectActionBuilder(requestName: String) extends ActionBuilder {
  private def components(protocolComponentsRegistry: ProtocolComponentsRegistry): SimpleChatComponents =
    protocolComponentsRegistry.components(SimpleChatProtocol.SimpleChatProtocolKey)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val statsEngine = coreComponents.statsEngine
    val component: SimpleChatComponents = components(protocolComponentsRegistry)
    new SimpleChatConnect(component.simpleChatProtocol,statsEngine,next)
  }
}
class SimpleChatConnect(protocol: SimpleChatProtocol, val statsEngine: StatsEngine, val next: Action)extends ExitableAction with NameGen{
  override def name: String = genName("upperConnect")

  override def execute(session: Session) = {
    implicit val sys = ActorSystem("mySystem")
    implicit val materializer = ActorMaterializer()
    val router = sys.actorOf(MessageRouter.props(statsEngine,session,name))
    val updatedSession = session.set("router",router)
    GraphMaker.tcpFlow(protocol.address, protocol.port,router).run()
    next ! updatedSession
  }
}

class SimpleChatDisconnectActionBuilder(requestName: String) extends ActionBuilder {
  private def components(protocolComponentsRegistry: ProtocolComponentsRegistry): SimpleChatComponents =
    protocolComponentsRegistry.components(SimpleChatProtocol.SimpleChatProtocolKey)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val statsEngine = coreComponents.statsEngine
    val component: SimpleChatComponents = components(protocolComponentsRegistry)
    new SimpleChatDisconnect(component.simpleChatProtocol,statsEngine,next)
  }
}
class SimpleChatDisconnect(protocol: SimpleChatProtocol, val statsEngine: StatsEngine, val next: Action)extends ExitableAction with NameGen{
  override def name: String = genName("upperConnect")

  override def execute(session: Session) = {
    val router = session("router").as[ActorRef]
    router ! "dc"
    router ! PoisonPill
    next ! session
  }
}

class SimpleChatSendMsgActionBuilder(requestName: String) extends ActionBuilder {
  private def components(protocolComponentsRegistry: ProtocolComponentsRegistry): SimpleChatComponents =
    protocolComponentsRegistry.components(SimpleChatProtocol.SimpleChatProtocolKey)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val statsEngine = coreComponents.statsEngine
    val component: SimpleChatComponents = components(protocolComponentsRegistry)
    new SimpleChatSendMsg(component.simpleChatProtocol,statsEngine,next)
  }
}
class SimpleChatSendMsg(protocol: SimpleChatProtocol, val statsEngine: StatsEngine, val next: Action)extends ExitableAction with NameGen{
  override def name: String = genName("upperConnect")

  override def execute(session: Session) = {
    val router = session("router").as[ActorRef]
    val recieverUid  = session("recieverUid").as[String]
    router ! recieverUid + ":" +System.currentTimeMillis()+">msg"
    next ! session
  }
}

class SimpleChatListUsersAndPickOneActionBuilder(requestName: String) extends ActionBuilder {
  private def components(protocolComponentsRegistry: ProtocolComponentsRegistry): SimpleChatComponents =
    protocolComponentsRegistry.components(SimpleChatProtocol.SimpleChatProtocolKey)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val statsEngine = coreComponents.statsEngine
    val component: SimpleChatComponents = components(protocolComponentsRegistry)
    new SimpleChatListUsersAndPickOne(component.simpleChatProtocol,statsEngine,next)
  }
}
class SimpleChatListUsersAndPickOne(protocol: SimpleChatProtocol, val statsEngine: StatsEngine, val next: Action)extends ExitableAction with NameGen{
  override def name: String = genName("upperConnect")

  override def execute(session: Session) = {
    implicit val timeout: Timeout = 3 second
    val router = session("router").as[ActorRef]
    val resp = router ? "list-u"
    val list = Await.result(resp, 3 second).asInstanceOf[String].split(";")
    val rand = new Random(System.currentTimeMillis())
    val random_index = rand.nextInt(list.length)
    val result = list(random_index)
    val updatedSession = session.set("recieverUid",result)
    next ! updatedSession
  }
}

object GraphMaker {
  import GraphDSL.Implicits._
  implicit val sys = ActorSystem("mySystem")
  def tcpFlow(ip: String, port: Int, router: ActorRef) = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    val source = b.add(Source.actorPublisher[ByteString](MessageSender.props(router)))
    val tcp = b.add(Tcp().outgoingConnection(ip, port))
    val sink= b.add(Sink.actorSubscriber(MessageReciever.props(router)))
    source ~> tcp ~> sink
    ClosedShape
  }
  )
}
