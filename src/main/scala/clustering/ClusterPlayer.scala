package clustering

import akka.actor.Actor
import scala.collection.mutable.Buffer
import akka.actor.Address
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.ReachabilityEvent
import akka.cluster.ClusterEvent.MemberEvent
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.MemberRemoved

case class Data(value: Double, time: Long)

object ClusterPlayer {
  case object LogData
}

class ClusterPlayer extends Actor {
  val system = ActorSystem.create("ClusterSystem",
    ConfigFactory.parseString(s"ClusterAwareRouter.akka.remote.netty.tcp.port=0")
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [player]"))
      .withFallback(ConfigFactory.load())
      .getConfig("ClusterAwareRouter"))
  val cluster = Cluster(system)

  val timeKeepers = Buffer[Address]()

  override def preStart() { cluster.subscribe(self, classOf[MemberUp], classOf[ReachabilityEvent]) }
  override def postStop() { cluster.unsubscribe(self) }

  def receive = {
    case ClusterPlayer.LogData =>
    case MemberUp(member) => {
      if (member.hasRole("timeKeeper")) timeKeepers += member.address
    }
    case MemberRemoved(member, previousState) => {
      println("timeKeeper added " + member.address)
      if (member.hasRole("timeKeeper")) timeKeepers -= member.address
    }
    case _ =>
  }

}