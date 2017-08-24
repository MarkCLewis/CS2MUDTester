package tester

import java.net.Socket
import java.io.{ BufferedReader, InputStreamReader, PrintStream }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ Props, ActorSystem }

/**
 * This application will test a networked MUD implementation. The user needs to provide connection
 * information, host and port, as well as information about what commands are to be tested.
 * 
 * -host
 * -port
 * -config
 * (-stress)
 * 
 * -nonnetworked
 */
object MUDTest extends App {
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

  // Read the configuration file
  val config = IOConfig(configFile)
  val system = ActorSystem("MUD")
 
  if(flagsAndValues.contains("-nonnetworked")) {
    println("Running nonnetworked test.")
    val in = Console.in
    val out = Console.out
    connectPlayer("MUDTest_SimplePlayer",in,out)
  } else {
    println("Running networked test.")
    if(flagsAndValues.contains("-stress")) {
      println("Running stress test.")
      val numStressPlayers = 2
      println("Connecting " + numStressPlayers + " players.")
      for(i <- 0 until numStressPlayers) {
        val sock = new Socket(flagsAndValues("-host").getOrElse("localhost"), flagsAndValues("-port").getOrElse("4000").toInt)
        val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
        val out = new PrintStream(sock.getOutputStream())
        connectPlayer("MUDTest_SimplePlayer_" + i,in,out)
      }
      println("Connected " + numStressPlayers + " players.")
    } else {
      println("Running accuracy test.")
      val sock = new Socket(flagsAndValues("-host").getOrElse("localhost"), flagsAndValues("-port").getOrElse("4000").toInt)
      val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
      val out = new PrintStream(sock.getOutputStream())
      connectPlayer("MUDTest_SimplePlayer",in,out)
    }
  }
  
  def connectPlayer(name:String,in:BufferedReader,out:PrintStream) {
    val player = system.actorOf(Props(SimplePlayer(name, in, out, config)), name)
    player ! Player.Connect
  }
}
