package gatling.runner

import gatling.protocol.Predef._
import io.gatling.core.Predef._
import io.gatling.core.feeder.RecordSeqFeederBuilder

import scala.concurrent.duration._

class ExampleSimulation extends Simulation {

  val simpleChatProtocol = sc.endpoint("127.0.0.1", 6000)
  val scn = scenario("test").
    exec(sc("user").connect).
    pause(1).
  exec(sc("user").pickOneUser())
    .pause(1)
    .repeat(50) {
      exec(sc("user").sendMsg())
        .pause(30 millis)
    }.
    exec(sc("user").disconnect())

  setUp(
    scn.inject(atOnceUsers(50))
  ).protocols(simpleChatProtocol)
}
