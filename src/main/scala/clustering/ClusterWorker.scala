package clustering

import akka.actor.Actor
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.cluster.Cluster

object ClusterWorker {
  case class Ping(value: Int) // rolls die
}

class ClusterWorker extends Actor {
  val config = ConfigFactory.load()
  implicit val system = ActorSystem("MUDStress")
  val cluster = Cluster(system)

  def receive = {
    case ClusterWorker.Ping(value) => {
      val route = s"${self.path.name}"

      if (util.Random.nextInt(4) == 1) {
        sender ! ClusterClient.Pong(value)
      } else {
        sender ! ClusterClient.Ping(value + 1)
      }
    }
    case _ => println("ClusterWorker received an odd message.")
  }

}