package tester

import akka.actor.ActorSystem
import java.io.{PrintStream,BufferedReader,InputStreamReader}
import akka.actor.Props
import java.net.Socket
import akka.actor.ActorRef
import scala.collection.mutable.Buffer
import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.PoisonPill

object PlayerManager {
  case object NonnetworkedTest
  case object NetworkedStress
  case object NetworkedTest
  case class ConnectSimplePlayers(n:Int)
  case class RegisterPlayer(player:ActorRef)
  case class DeregisterPlayer(player:ActorRef)
  
  def apply(c:IOConfig,s:ActorSystem,fAV:Map[String,Option[String]]):PlayerManager = {
    new PlayerManager(c,s,fAV)
  }
}

class PlayerManager private(private val config:IOConfig,
    private val system:ActorSystem,
    private val flagsAndValues:Map[String,Option[String]]) extends Actor {
  
  implicit private val ec = context.system.dispatcher
  private val players = Buffer[ActorRef]()
  private var playerNumber = 0
  private val timeKeeper = system.actorOf(Props(TimeKeeper()),"TimeKeeper")
  
  if(flagsAndValues.contains("-nonnetworked")) {
    self ! PlayerManager.NonnetworkedTest
  } else {
    if(flagsAndValues.contains("-stress")) {
      self ! PlayerManager.NetworkedStress
    } else {
      self ! PlayerManager.NetworkedTest
    }
  }
  
  def receive() = {
    case PlayerManager.NonnetworkedTest => startNonnetworkedTest()
    case PlayerManager.NetworkedStress => startNetworkedStress()
    case PlayerManager.NetworkedTest => startNetworkedTest()
    case PlayerManager.ConnectSimplePlayers(n) => connectSimplePlayers(n)
    case PlayerManager.RegisterPlayer(player) => {
      players += player
      timeKeeper ! TimeKeeper.PlayerRegistered
    }
    case PlayerManager.DeregisterPlayer(player) => {
      players -= player
      sender ! Player.Disconnect
    }
    case _ =>
  }
  
  private def startNonnetworkedTest() {
    println("Running nonnetworked test.")
    val in = Console.in
    val out = Console.out
    connectSimplePlayer("MUDTest_SimplePlayer",in,out)
  }
  
  private def startNetworkedTest() {
    println("Running networked procedure test.")
    if(config.testProcs("getDropProc")) {
      println("Running get/drop procedure.")
      val sock = new Socket(flagsAndValues("-host").getOrElse("localhost"), flagsAndValues("-port").getOrElse("4000").toInt)
      val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
      val out = new PrintStream(sock.getOutputStream())
      val player = connectTestPlayer("MUDTest_TestPlayer",in,out)
      player ! TestPlayer.GetDropTest
    }
  }
  
  private def startNetworkedStress() {
    val numInitialPlayers = 50
    val numIntervalPlayers = 50
    val addPlayerInterval = 10 seconds
  
    println("Running networked stress test.")
    println("Connecting " + numInitialPlayers + " players.")
    
    self ! PlayerManager.ConnectSimplePlayers(numInitialPlayers)
    context.system.scheduler.schedule(1 seconds, addPlayerInterval, self, PlayerManager.ConnectSimplePlayers(numIntervalPlayers))
  }
  
  private def connectSimplePlayer(name:String,in:BufferedReader,out:PrintStream):ActorRef = {
    val actorName = name + "_" + playerNumber
    val player = system.actorOf(Props(SimplePlayer(actorName, in, out, config, self, timeKeeper)), actorName)
    playerNumber+=1
    player ! Player.Connect
    player
  }
  
  private def connectSimplePlayers(n:Int) {
    for(i <- 0 until n) {
      val sock = new Socket(flagsAndValues("-host").getOrElse("localhost"), flagsAndValues("-port").getOrElse("4000").toInt)
      val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
      val out = new PrintStream(sock.getOutputStream())
      connectSimplePlayer("MUDTest_SimplePlayer",in,out)
    }
  }
  
  private def connectTestPlayer(name:String,in:BufferedReader,out:PrintStream):ActorRef = {
    val actorName = name + "_" + playerNumber
    val player = system.actorOf(Props(TestPlayer(actorName, in, out, config)),actorName)
    playerNumber+=1
    player ! Player.Connect
    player
  }
}
