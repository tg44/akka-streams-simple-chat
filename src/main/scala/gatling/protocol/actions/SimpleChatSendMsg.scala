package gatling.protocol.actions

import akka.actor._
import gatling.protocol.{SimpleChatComponents, SimpleChatProtocol}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

class SimpleChatSendMsgActionBuilder(requestName: String) extends ActionBuilder {
	private def components(protocolComponentsRegistry: ProtocolComponentsRegistry): SimpleChatComponents =
		protocolComponentsRegistry.components(SimpleChatProtocol.SimpleChatProtocolKey)

	override def build(ctx: ScenarioContext, next: Action): Action = {
		import ctx._
		val statsEngine = coreComponents.statsEngine
		val component: SimpleChatComponents = components(protocolComponentsRegistry)
		new SimpleChatSendMsg(component.simpleChatProtocol, statsEngine, next)
	}
}

class SimpleChatSendMsg(protocol: SimpleChatProtocol, val statsEngine: StatsEngine, val next: Action) extends ExitableAction with NameGen {
	override def name: String = genName("upperConnect")

	override def execute(session: Session) = {
		val router = session("router").as[ActorRef]
		val recieverUid = session("recieverUid").as[String]
		router ! recieverUid + ":" + System.currentTimeMillis() + ">msg"
		next ! session
	}
}


