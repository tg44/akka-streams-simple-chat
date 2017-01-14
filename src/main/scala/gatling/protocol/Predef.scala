package gatling.protocol

/**
  * Created by Törcsi on 2017. 01. 14..
  */
object Predef {
  val sc = SimpleChatProtocolBuilder

  implicit def simpleChatBuilderToProtocol(builder: SimpleChatProtocolBuilder): SimpleChatProtocol = builder.build()

  def sc(name: String) = new SimpleChatUser(name)
}
