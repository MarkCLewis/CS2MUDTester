package stresser.remoting

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.Socket

import scala.collection.mutable.Buffer
import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Address
import akka.actor.Deploy
import akka.actor.Props
import akka.remote.RemoteScope
import utility.ActorMessages
import utility.Player
import utility.PlayerManager

object StressPlayerManager {
  def apply(info: StressTestInfo, stressTestManager:ActorRef, timeKeeper: ActorRef, ip: String): StressPlayerManager = {
    new StressPlayerManager(info, stressTestManager, timeKeeper, ip)
  }
}

class StressPlayerManager private (private val info: StressTestInfo,
    private val stressTestManager: ActorRef,
    private val timeKeeper: ActorRef,
    private val ip: String) extends Actor {

  private val players = Buffer[ActorRef]()
  private var playerNumber = 0
  val address = Address("akka.tcp", "MUDStressTest", ip, 5150)

  private val numInitialPlayers = 10
  private val numIntervalPlayers = 5
  private val addPlayerInterval = 1 second

  implicit private val ec = context.system.dispatcher //s.lookup("prio-dispatcher")
  self ! PlayerManager.ConnectSimplePlayers(numInitialPlayers)
  private val schedule = context.system.scheduler.schedule(1 seconds, addPlayerInterval, self, PlayerManager.ConnectSimplePlayers(numIntervalPlayers))

  def receive() = {
    case PlayerManager.ConnectSimplePlayers(n) => {
      connectStressPlayers(n)
      timeKeeper ! TimeKeeper.ChangeNumPlayers(n)
    }
    case ActorMessages.EndStressTest => {
      players.foreach(_ ! ActorMessages.EndStressTest)
      context.stop(self)
    }
    case _ =>
  }

  private def connectStressPlayers(n: Int) {
    try {
      for (i <- 0 until n) {
        val sock = new Socket(info.host, info.port)
        val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
        val out = new PrintStream(sock.getOutputStream())
        val actorName = "StressPlayer_" + ip + "_" + playerNumber
        val player = context.system.actorOf(Props(StressPlayer(actorName, in, out, info, stressTestManager, timeKeeper))
          .withDeploy(Deploy(scope = RemoteScope(address))) //.withDispatcher("prio-dispatcher"), actorName)
          )
        playerNumber += 1
        players += player
        player ! Player.Connect
        //player
      }
    } catch {
      case e: java.net.SocketException => stressTestManager ! ActorMessages.EmergencyShutdown(info)
    }
  }
}