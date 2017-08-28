package tester

import akka.actor.ActorSystem
import java.io.{PrintStream,BufferedReader,InputStreamReader}
import akka.actor.Props
import java.net.Socket

object PlayerManager {
  def apply(c:IOConfig,s:ActorSystem,fAV:Map[String,Option[String]]):PlayerManager = {
    new PlayerManager(c,s,fAV)
  }
}

class PlayerManager private(private val config:IOConfig,
    private val system:ActorSystem,
    private val flagsAndValues:Map[String,Option[String]]){
  
  if(flagsAndValues.contains("-nonnetworked")) {
    nonnetworkedTest()
  } else {
    if(flagsAndValues.contains("-stress")) {
      networkedStress()
    } else {
      networkedTest()
    }
  }
  
  def nonnetworkedTest() {
    println("Running nonnetworked test.")
    val in = Console.in
    val out = Console.out
    connectSimplePlayer("MUDTest_SimplePlayer",in,out)
  }
  
  def networkedTest() {
    println("Running networked test.")
    println("Running test test.")
    val sock = new Socket(flagsAndValues("-host").getOrElse("localhost"), flagsAndValues("-port").getOrElse("4000").toInt)
    val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
    val out = new PrintStream(sock.getOutputStream())
    connectTestPlayer("MUDTest_TestPlayer",in,out)
  }
  
  def networkedStress() {
    println("Running networked test.")
    println("Running stress test.")
    val numStressPlayers = 1
    println("Connecting " + numStressPlayers + " players.")
    for(i <- 0 until numStressPlayers) {
      val sock = new Socket(flagsAndValues("-host").getOrElse("localhost"), flagsAndValues("-port").getOrElse("4000").toInt)
      val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
      val out = new PrintStream(sock.getOutputStream())
      connectSimplePlayer("MUDTest_SimplePlayer_" + i,in,out)
    }
    println("Connected " + numStressPlayers + " players.")
  }
  
  def connectSimplePlayer(name:String,in:BufferedReader,out:PrintStream) {
    val player = system.actorOf(Props(SimplePlayer(name, in, out, config)), name)
    player ! Player.Connect
  }
  
  def connectTestPlayer(name:String,in:BufferedReader,out:PrintStream) {
    val player = system.actorOf(Props(TestPlayer(name, in, out, config)), name)
    player ! Player.Connect
  }
}