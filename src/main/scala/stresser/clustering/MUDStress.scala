package stresser.clustering

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.actor.Props
import utility.IOConfig
import java.net.InetAddress
import akka.actor.Deploy
import akka.actor.{ Props, Deploy, Address, AddressFromURIString }
import akka.remote.RemoteScope

object MUDStress extends App {
  println("args: " + args.mkString(", "))

  val defaultHost = "localhost"
  val defaultPort = "4000"
  val defaultConfig = "config.xml"
  val flagsAndValues = Map("-host" -> Some(defaultHost), "-port" -> Some(defaultPort), "-config" -> Some(defaultConfig)) ++
    args.zipAll(args.tail, "", "").foldLeft(Map[String, Option[String]]()) { (m, t) =>
      if (t._1(0) == '-' && t._2.isEmpty) {
        m + (t._1 -> None)
      } else if (t._1(0) == '-' && t._2(0) != '-') {
        m + (t._1 -> Some(t._2))
      } else if (t._1(0) == '-') {
        m + (t._1 -> None)
      } else {
        m
      }
    }
  val requiredFlags = Array.fill(0)("") //"-host -port -config".split(" ")
  val allRequired = for (flag <- requiredFlags) yield {
    if (flagsAndValues.contains(flag)) {
      flagsAndValues(flag) match {
        case Some(value) => true
        case None => {
          println("Must give a value for " + flag + ".")
          false
        }
      }
    } else {
      println(flag + " is a required setting.")
      false
    }
  }
  if (allRequired.exists(!_)) sys.exit(1)

  val configFile = if (flagsAndValues.contains("-config")) {
    flagsAndValues("-config") match {
      case Some(value) => value
      case None => {
        println("-config without file, defaulting to config.xml.")
        "config.xml"
      }
    }
  } else {
    println("No -config specified, defaulting to config.xml.")
    "config.xml"
  }

  // Akka Clustering Stuff
  val clusterPort = if(!args.isEmpty) {
    args(0) match {
      case "pandora00" => 2552
      case "pandora01" => 2553
      case "pandora02" => 2554
      case "pandora03" => 2555
      case "pandora04" => 2556
      case "pandora05" => 2557
      case "pandora06" => 2558
      case "pandora07" => 2559
      case "pandora08" => 2560
      case _ => 0
    }
  } else 0

  val system = ActorSystem.create("ClusterSystem",
    ConfigFactory.parseString(s"ClusterAwareRouter.akka.remote.netty.tcp.port=${clusterPort}")
      .withFallback(ConfigFactory.load())
      .getConfig("ClusterAwareRouter"))

  // Read the configuration file
  val ioConfig = IOConfig(configFile)
  val config = ConfigFactory.empty()
  val host = flagsAndValues("-host").getOrElse(defaultHost)
  val port = flagsAndValues("-port").getOrElse(defaultPort).toInt
  val playerManager = system.actorOf(Props(StressPlayerManager(ioConfig, system, host, port)), "StressPlayerManager_" + InetAddress.getLocalHost().toString.split("/")(1))
}