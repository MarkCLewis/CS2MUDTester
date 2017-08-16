package tester

import java.net.Socket
import java.io.{BufferedReader,InputStreamReader,PrintStream}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Props,ActorSystem}

/**
 * This application will test a networked MUD implementation. The user needs to provide connection
 * information, host and port, as well as information about what commands are to be tested.
 */
object MUDTest extends App {
  val flagsAndValues = args.zipAll(args.tail,"","").foldLeft(Map[String,Option[String]]()) { (m, t) =>
    if(t._1(0) == '-' && t._2.isEmpty) {
      m + (t._1 -> None)
    } else if(t._1(0) == '-' && t._2(0) != '-') {
      m + (t._1 -> Some(t._2))
    } else if(t._1(0) == '-') {
      m + (t._1 -> None)
    } else {
      m
    }
  }
  val requiredFlags = "-host -port -config".split(" ")
  val allRequired = for(flag <- requiredFlags) yield {
    if(flagsAndValues.contains(flag)) {
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
  if(allRequired.exists(!_)) sys.exit(1)
  
  val configFile = if(flagsAndValues.contains("-config")) {
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

  
  // Figure out which commands we are testing.
  val commands = IOConfig(configFile)

  val sock = new Socket(flagsAndValues("-host").getOrElse("localhost"), flagsAndValues("-port").getOrElse("-1").toInt)
  val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
  val out = new PrintStream(sock.getOutputStream())
  
  val system = ActorSystem("MUD")
  val name = "MUDTESTPLAYER"
  val player = system.actorOf(Props(MUDTestPlayer(name,sock,in,out)),name)
}

// TODO - need an abstraction for connections.