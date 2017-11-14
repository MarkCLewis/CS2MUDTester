package tester

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.Socket

import scala.collection.mutable.Buffer

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import utility.IOConfig
import utility.Player
import utility.PlayerManager
import utility.Command

object TestPlayerManager {
  case class PositiveTestResult(test: Test)
  case class NegativeTestResult(test: Test, command: Command)

  def apply(config: IOConfig, system: ActorSystem, flagsAndValues: Map[String, Option[String]]): TestPlayerManager = {
    new TestPlayerManager(config, system, flagsAndValues)
  }
}

class TestPlayerManager private (private val config: IOConfig,
    private val system: ActorSystem,
    private val flagsAndValues: Map[String, Option[String]]) extends Actor {

  implicit private val ec = context.system.dispatcher
  private val players = Buffer[ActorRef]()
  private var playerNumber = 0

  startNetworkedTest()

  def receive() = {
    case PlayerManager.RegisterPlayer(player) => players += player
    case PlayerManager.DeregisterPlayer(player) => {
      if (players.contains(player)) players -= player
      sender ! Player.Disconnect
    }
    case TestPlayerManager.PositiveTestResult(test) => println(test.success())
    case TestPlayerManager.NegativeTestResult(test, command) => println(test.fail(command))
    case _ =>
  }

  private def startNetworkedTest() {
    println("Running networked procedure test.")
    if (config.testProcs("getDrop")) {
      val player = connectTestPlayer("MUDTest_TestPlayer")
      player ! TestPlayer.GetDropTest
    }
  }

  private def connectTestPlayer(name: String): ActorRef = {
    val sock = new Socket(flagsAndValues("-host").getOrElse("localhost"), flagsAndValues("-port").getOrElse("4000").toInt)
    val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
    val out = new PrintStream(sock.getOutputStream())
    val actorName = name + "_" + playerNumber
    val player = system.actorOf(Props(TestPlayer(actorName, in, out, config)), actorName)
    playerNumber += 1
    player ! Player.Connect
    player
  }
}
