package gatling.protocol

import akka.actor.ActorSystem
import io.gatling.core
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{ProtocolKey, ProtocolComponents, Protocol}
import io.gatling.core.session.Session


class SimpleChatProtocol (val address: String, val port: Int)extends Protocol {
  type Components = SimpleChatComponents
}

case class SimpleChatComponents(simpleChatProtocol: SimpleChatProtocol) extends ProtocolComponents {
  def onStart: Option[Session => Session] = None
  def onExit: Option[Session => Unit] = None
}

case class SimpleChatProtocolBuilder(address: String, port: Int) {
  def build() = SimpleChatProtocol(address, port)
}

object SimpleChatProtocolBuilder {
  def endpoint(address: String, port: Int) = SimpleChatProtocolBuilder(address, port)
}

object SimpleChatProtocol {
  def apply(address: String, port: Int) = new SimpleChatProtocol(address, port)

  val SimpleChatProtocolKey = new ProtocolKey {

    type Protocol = SimpleChatProtocol
    type Components = SimpleChatComponents

    override def protocolClass: Class[core.protocol.Protocol] = classOf[SimpleChatProtocol].asInstanceOf[Class[io.gatling.core.protocol.Protocol]]

    override def defaultProtocolValue(configuration: GatlingConfiguration): SimpleChatProtocol = throw new IllegalStateException("Can't provide a default value for SimpleChatProtocol")

    override def newComponents(system: ActorSystem, coreComponents: CoreComponents): SimpleChatProtocol => SimpleChatComponents = {
      simpleChatProtocol => SimpleChatComponents(simpleChatProtocol)
    }
  }
}