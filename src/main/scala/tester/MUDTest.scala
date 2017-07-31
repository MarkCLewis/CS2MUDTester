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
  val flagsAndValues = args.zip(args.tail).foldLeft(Map[String, String]()) { (m, t) =>
    if(t._1(0) == '-') m + (t._1 -> t._2) else m
  }
  val requiredFlags = "-host -port".split(" ")
  val allRequired = for(flag <- requiredFlags) yield {
    if(!flagsAndValues.contains(flag)) {
      println(flag + " is a required setting.")
      false
    } else true
  }
  if(allRequired.exists(!_)) sys.exit(1)
  
  // Figure out which commands we are testing.

  val sock = new Socket(flagsAndValues("-host"), flagsAndValues("-port").toInt)
  val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
  val out = new PrintStream(sock.getOutputStream())
  
  val system = ActorSystem("MUD")
  val name = "MUDTESTPLAYER"
  val player = system.actorOf(Props(MUDTestPlayer(name,sock,in,out)),name)
}

// TODO - need an abstraction for commands.
// TODO - need an abstraction for connections.