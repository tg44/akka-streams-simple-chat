package gatling.protocol.actions

import akka.actor.ActorRef
import akka.util.Timeout
import gatling.protocol.{SimpleChatComponents, SimpleChatProtocol}
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

import scala.concurrent.Await
import scala.util.Random
import scala.concurrent.duration._
import akka.pattern.ask



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
