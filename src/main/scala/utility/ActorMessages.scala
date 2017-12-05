package utility

import stresser.remoting.StressTestInfo

object ActorMessages {
  case object EndStressTest
  case class EndStressTest(info: StressTestInfo)
  case class EmergencyShutdown(info: StressTestInfo)
}