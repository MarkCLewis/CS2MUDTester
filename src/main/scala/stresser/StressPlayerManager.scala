package stresser

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.Socket

import scala.collection.mutable.Buffer
import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import utility.IOConfig
import utility.Player
import utility.PlayerManager

object StressPlayerManager {
  case class ConnectSimplePlayers(n: Int)

  def apply(c: IOConfig, s: ActorSystem, fAV: Map[String, Option[String]]): StressPlayerManager = {
    new StressPlayerManager(c, s, fAV)
  }
}

class StressPlayerManager private (private val config: IOConfig,
    private val system: ActorSystem,
    private val flagsAndValues: Map[String, Option[String]]) extends Actor {

  implicit private val ec = context.system.dispatcher
  private val players = Buffer[ActorRef]()
  private var playerNumber = 0
  private val timeKeeper = system.actorOf(Props(TimeKeeper()), "TimeKeeper")

  startNetworkedStress()

  def receive() = {
    case StressPlayerManager.ConnectSimplePlayers(n) => connectSimplePlayers(n)
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

  private def startNetworkedStress() {
    val numInitialPlayers = 50
    val numIntervalPlayers = 50
    val addPlayerInterval = 10 seconds

    println("Running networked stress test.")
    println("Connecting " + numInitialPlayers + " players.")

    self ! StressPlayerManager.ConnectSimplePlayers(numInitialPlayers)
    context.system.scheduler.schedule(1 seconds, addPlayerInterval, self, StressPlayerManager.ConnectSimplePlayers(numIntervalPlayers))
  }

  private def connectSimplePlayer(name: String, in: BufferedReader, out: PrintStream): ActorRef = {
    val actorName = name + "_" + playerNumber
    val player = system.actorOf(Props(StressPlayer(actorName, in, out, config, self, timeKeeper)), actorName)
    playerNumber += 1
    player ! Player.Connect
    player
  }

  private def connectSimplePlayers(n: Int) {
    for (i <- 0 until n) {
      val sock = new Socket(flagsAndValues("-host").getOrElse("localhost"), flagsAndValues("-port").getOrElse("4000").toInt)
      val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
      val out = new PrintStream(sock.getOutputStream())
      connectSimplePlayer("MUDTest_SimplePlayer", in, out)
    }
  }
}