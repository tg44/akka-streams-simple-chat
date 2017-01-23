/**
	* Created by torcsi on 23/01/2017.
	*/
package object server {
	class InMsg()
	case class Message(payload: String, uid: String) extends InMsg
	case class ListUsers() extends InMsg
	case class Disconnect() extends InMsg
	class InControl() extends InMsg
	case class MyUid(uid: String) extends InControl
	case class CtrlListUsers(uid: String) extends InControl
	case class CtrlDisconnect(uid: String) extends InControl
	case class CtrlSignUp(uid: String) extends InControl
}
