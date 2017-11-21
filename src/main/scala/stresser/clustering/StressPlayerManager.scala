package stresser.clustering

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.InetAddress
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
import utility.ResponseReport

object StressPlayerManager {
  case class ConnectSimplePlayers(n: Int)
  case class ReceiveResponseReport(report: ResponseReport)

  def apply(config: IOConfig, system: ActorSystem, host: String, port: Int): StressPlayerManager = {
    new StressPlayerManager(config, system, host, port)
  }
}

class StressPlayerManager private (private val config: IOConfig,
    private val system: ActorSystem,
    private val host: String,
    private val port: Int) extends Actor {

  implicit private val ec = context.system.dispatcher
  private val players = Buffer[ActorRef]()
  private var numPlayers = 0
  private var playerNumber = 0
  private val globalName = "_" + InetAddress.getLocalHost().toString.split("/")(1)
  private val timeKeeper = system.actorOf(Props(TimeKeeper(self)), "TimeKeeper" + globalName)

  startNetworkedStress()

  def receive() = {
    case StressPlayerManager.ConnectSimplePlayers(n) => {
      connectSimplePlayers(n)
      numPlayers += n
    }
    case PlayerManager.DeregisterPlayer(player) => {
      players -= player
      context.stop(player)
      numPlayers -= 1
    }
    case StressPlayerManager.ReceiveResponseReport(report) => println("Number of Players: " + numPlayers + ", number of commands: " + report.numResponses + ", average response time: " + report.average / 1000000.0 + " ms")
    case _ =>
  }

  private def startNetworkedStress() {
    val numInitialPlayers = 10
    val numIntervalPlayers = 10
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
    players += player
    player ! Player.Connect
    player
  }

  private def connectSimplePlayers(n: Int) {
    for (i <- 0 until n) {
      val sock = new Socket(host, port)
      val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
      val out = new PrintStream(sock.getOutputStream())
      connectSimplePlayer("StressPlayer" + globalName, in, out)
    }
  }
}