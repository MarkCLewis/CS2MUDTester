package clustering

import akka.actor.Actor
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object ClusterTimeKeeper {
  case object GatherData
  case class GatheredData(dataSum: Double, dataCount: Int)
  case object AggregateTime
}

class ClusterTimeKeeper extends Actor {
  val system = ActorSystem.create("ClusterSystem",
    ConfigFactory.parseString(s"ClusterAwareRouter.akka.remote.netty.tcp.port=0")
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [timeKeeper]"))
      .withFallback(ConfigFactory.load())
      .getConfig("ClusterAwareRouter"))

  var sum = 0.0
  var count = 0

  def receive = {
    case ClusterTimeKeeper.GatherData =>
    case ClusterTimeKeeper.GatheredData(dataSum, dataCount) => {
      sum += dataSum
      count += dataCount
    }
    case ClusterTimeKeeper.AggregateTime => aggregateTime()
    case _ =>
  }

  private def aggregateTime() {
    println("Average: " + sum / count)
  }
}