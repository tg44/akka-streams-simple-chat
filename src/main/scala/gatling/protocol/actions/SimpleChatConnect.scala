package gatling.protocol.actions

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import gatling.protocol.{SimpleChatComponents, SimpleChatProtocol}
import gatling.protocol.protoclient.{GraphMaker, MessageRouter}
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

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
