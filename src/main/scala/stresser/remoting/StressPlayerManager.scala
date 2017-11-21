package stresser.remoting

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.InetAddress
import java.net.Socket

import scala.collection.mutable.Buffer
import scala.concurrent.duration._

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import utility.IOConfig
import utility.Player
import utility.PlayerManager

object StressPlayerManager {

  def apply(config: IOConfig, host: String, port: Int, timeKeeper: ActorRef, ip:String): StressPlayerManager = {
    new StressPlayerManager(config, host, port, timeKeeper, ip)
  }
}

class StressPlayerManager private (private val config: IOConfig,
    private val host: String,
    private val port: Int,
    private val timeKeeper: ActorRef,
    private val ip:String) extends Actor {

  implicit private val ec = context.system.dispatcher
  private val players = Buffer[ActorRef]()
  private var playerNumber = 0

  private val numInitialPlayers = 10
  private val numIntervalPlayers = 10
  private val addPlayerInterval = 10 seconds

  self ! PlayerManager.ConnectSimplePlayers(numInitialPlayers)
  context.system.scheduler.schedule(1 seconds, addPlayerInterval, self, PlayerManager.ConnectSimplePlayers(numIntervalPlayers))
  
  def receive() = {
    case PlayerManager.ConnectSimplePlayers(n) => connectSimplePlayers(n)
    case PlayerManager.DeregisterPlayer(player) => {
      players -= player
      context.stop(player)
    }
    case _ =>
  }

  private def connectSimplePlayer(in: BufferedReader, out: PrintStream): ActorRef = {
    val actorName = "StressPlayer_" + ip + "_" + playerNumber
    val player = context.system.actorOf(Props(StressPlayer(actorName, in, out, config, self, timeKeeper)), actorName)
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
      connectSimplePlayer(in, out)
    }
  }
}