package clustering

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.actor.Props
import akka.routing.FromConfig

object ClusterAwareRouterApp {

  def main(args: Array[String]): Unit = {

    var port = 0
    if (!args.isEmpty && (args(0).equals("seednode"))) port = 2552;

    val system = ActorSystem.create("ClusterSystem", ConfigFactory
      .parseString(s"ClusterAwareRouter.akka.remote.netty.tcp.port=${port}")
      .withFallback(ConfigFactory.load())
      .getConfig("ClusterAwareRouter"))

    val randomRouter = system.actorOf(Props[EchoActor].
      withRouter(FromConfig()), name = "ClusterAwareActor")

    Thread.sleep(10000)

    1 to 10 foreach {
      i => randomRouter ! i
    }
  }
}