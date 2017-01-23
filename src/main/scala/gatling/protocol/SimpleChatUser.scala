package gatling.protocol

import gatling.protocol.actions.{SimpleChatConnectActionBuilder, SimpleChatDisconnectActionBuilder, SimpleChatListUsersAndPickOneActionBuilder, SimpleChatSendMsgActionBuilder}

class SimpleChatUser(requestName: String) {
  def connect() = new SimpleChatConnectActionBuilder(requestName)
  def disconnect() = new SimpleChatDisconnectActionBuilder(requestName)
  def pickOneUser() = new SimpleChatListUsersAndPickOneActionBuilder(requestName)
  def sendMsg() = new SimpleChatSendMsgActionBuilder(requestName)
}
