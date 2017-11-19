package clustering

import akka.actor.ActorSystem
import akka.cluster.Cluster

object ClusterMain extends App {
  val system = ActorSystem("MUDStress")
  val cluster = Cluster(system)

}