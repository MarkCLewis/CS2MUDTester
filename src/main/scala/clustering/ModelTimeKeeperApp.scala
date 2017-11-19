package clustering

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.actor.Props
import akka.routing.FromConfig
import java.net.InetAddress

object ModelTimeKeeperApp extends App {
  var port = 0
  if (!args.isEmpty && (args(0).equals("seednode"))) port = 2552;

  val system = ActorSystem.create("ClusterSystem",
    ConfigFactory.parseString(s"ClusterAwareRouter.akka.remote.netty.tcp.port=${port}")
      .withFallback(ConfigFactory.load())
      .getConfig("ClusterAwareRouter"))

  val timeKeeper = system.actorOf(Props(ModelTimeKeeper()), name = "MUDStress_TimeKeeper_" + InetAddress.getLocalHost().toString.split("/")(1))
  val players = for (i <- 0 until 10) yield system.actorOf(Props(ModelPlayer(timeKeeper)), "MUDStress_StressPlayer_" + InetAddress.getLocalHost().toString.split("/")(1) + "_" + i)

}