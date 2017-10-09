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
  case object CalculateResponseTime
  case class ReceiveResponse(response:Response)
  case class RegisterPlayer(player:ActorRef)
  case class DeregisterPlayer(player:ActorRef)
  
  def apply(c:IOConfig,s:ActorSystem,fAV:Map[String,Option[String]]):PlayerManager = {
    new PlayerManager(c,s,fAV)
  }
}

class PlayerManager private(private val config:IOConfig,
    private val system:ActorSystem,
    private val flagsAndValues:Map[String,Option[String]]) extends Actor {
  
  private val players = Buffer[ActorRef]()
  private val responses = Buffer[Response]()
  private var playerNumber = 0
  
  implicit private val ec = context.system.dispatcher
  context.system.scheduler.schedule(1 seconds, 1 seconds, self, PlayerManager.CalculateResponseTime)
  
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
    case PlayerManager.CalculateResponseTime => calculateResponseTime()
    case PlayerManager.ReceiveResponse(response) => responses += response
    case PlayerManager.RegisterPlayer(player) => players += player
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
    val numInitialPlayers = 10
    val numIntervalPlayers = 10
  
    println("Running networked stress test.")
    println("Connecting " + numInitialPlayers + " players.")
    
    self ! PlayerManager.ConnectSimplePlayers(numInitialPlayers)
    context.system.scheduler.schedule(1 seconds, 10 seconds, self, PlayerManager.ConnectSimplePlayers(numIntervalPlayers))
  }
  
  private def connectSimplePlayers(n:Int) {
    for(i <- 0 until n) {
      val sock = new Socket(flagsAndValues("-host").getOrElse("localhost"), flagsAndValues("-port").getOrElse("4000").toInt)
      val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
      val out = new PrintStream(sock.getOutputStream())
      connectSimplePlayer("MUDTest_SimplePlayer",in,out)
    }
  }
  
  private def connectSimplePlayer(name:String,in:BufferedReader,out:PrintStream):ActorRef = {
    val actorName = name + "_" + playerNumber
    val player = system.actorOf(Props(SimplePlayer(actorName, in, out, config, self)), actorName)
    playerNumber+=1
    player ! Player.Connect
    player
  }
  
  private def connectTestPlayer(name:String,in:BufferedReader,out:PrintStream):ActorRef = {
    val actorName = name + "_" + playerNumber
    val player = system.actorOf(Props(TestPlayer(actorName, in, out, config)),actorName)
    playerNumber+=1
    player ! Player.Connect
    player
  }
  
  private def calculateResponseTime() {
    if(!responses.isEmpty) {
      val average = responses.map(_.time).sum/responses.length
      println("Number of Players: " + players.length + ", number of commands: " + responses.length + ", average response time: " + average/1000000 + " ms")
      responses.clear()
    }
  }
}