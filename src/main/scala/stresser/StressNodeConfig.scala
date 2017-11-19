package stresser

import akka.remote.testkit.MultiNodeConfig

object StressNodeConfig extends MultiNodeConfig {
  val seedNode = role("seednode")
  val nodes = Array.fill(8)(role("node"))
}