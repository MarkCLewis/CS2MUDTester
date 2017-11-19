package clustering

import akka.actor.Actor
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.actor.ActorLogging
import akka.actor.Address
import akka.actor.ActorRef
import akka.cluster.ClusterEvent.MemberUp
import akka.actor.Props
import akka.actor.Deploy
import akka.remote.RemoteScope
import akka.actor.RootActorPath
import akka.cluster.ClusterEvent.ReachabilityEvent
import akka.actor.RelativeActorPath
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.ClusterEvent.MemberEvent
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.ReachableMember
import scala.concurrent.duration._
import java.util.concurrent.ThreadLocalRandom
import com.typesafe.config.ConfigFactory
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.RelativeActorPath
import akka.actor.RootActorPath
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.MemberStatus

object ClusterClient {
  case class Ping(value: Int) // forwards message to random worker actor
  case class Pong(value: Int) // logs message

  val config = ConfigFactory.load()
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("MUDStress")
    system.actorOf(Props(classOf[ClusterClient], "/user/statsService"), "client")
  }
}

class ClusterClient(servicePath: String) extends Actor with ActorLogging {
  val cluster = Cluster(context.system)
  val servicePathElements = servicePath match {
    case RelativeActorPath(elements) => elements
    case _ => throw new IllegalArgumentException(
      "servicePath [%s] is not a valid relative actor path" format servicePath)
  }
  import context.dispatcher
  val tickTask = context.system.scheduler.schedule(2.seconds, 2.seconds, self, "tick")

  var nodes = Set[Address]()

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent], classOf[ReachabilityEvent])
  }
  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    tickTask.cancel()
  }

  def receive = {
    case state: CurrentClusterState =>
      nodes = state.members.collect {
        case m if m.hasRole("compute") && m.status == MemberStatus.Up => m.address
      }
    case MemberUp(m) => {
      if (m.hasRole("compute")) {
        nodes += m.address
      }
    }
    case other: MemberEvent => nodes -= other.member.address
    case UnreachableMember(m) => nodes -= m.address
    case ReachableMember(m) if m.hasRole("compute") => nodes += m.address
  }

}