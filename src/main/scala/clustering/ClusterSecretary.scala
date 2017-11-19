package clustering

import akka.actor.Actor
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object ClusterSecretary {
  case class ReceiveData(data: Double)
  case object GatherData
}

class ClusterSecretary extends Actor {
  val system = ActorSystem.create("ClusterSystem", ConfigFactory
    .parseString(s"ClusterAwareRouter.akka.remote.netty.tcp.port=0")
    .withFallback(ConfigFactory.load())
    .getConfig("ClusterAwareRouter"))

  var sum = 0.0
  var count = 0

  def receive = {
    case ClusterSecretary.ReceiveData(data) => {
      sum += data
      count += 1
    }
    case ClusterSecretary.GatherData => sender ! ClusterTimeKeeper.GatheredData(sum, count)
    case _ =>
  }
}