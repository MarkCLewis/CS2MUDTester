package stresser

import java.io.BufferedReader
import java.io.PrintStream

import scala.collection.Seq
import scala.collection.mutable.Buffer
import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import utility.Command
import utility.Debug
import utility.IOConfig
import utility.Player
import utility.PlayerManager

case class Response(time: Long, result: Boolean)

object StressPlayer {
  case object TakeAction

  def apply(n: String, i: BufferedReader, o: PrintStream, config: IOConfig, playerManager: ActorRef, timeKeeper: ActorRef): StressPlayer = {
    new StressPlayer(n, i, o, config, playerManager, timeKeeper)
  }
}

class StressPlayer private (name: String,
    private val in: BufferedReader,
    private val out: PrintStream,
    private val config: IOConfig,
    private val playerManager: ActorRef,
    private val timeKeeper: ActorRef) extends Actor {

  protected var gs = Player.GameState("", Nil, Nil, Nil, Nil)
  private var commandCount = 0
  private val responses = Buffer[Response]()

  def receive() = {
    case Player.Connect => connect()
    case Player.Disconnect => disconnect()
    case StressPlayer.TakeAction => takeAction()
    case _ =>
  }

  private def connect() {
    // Tell name for login
    out.println(name)

    // Read initial room description
    Command.readToMatch(in, config.roomOutput) match {
      case Left(message) => println(message)
      case Right(m) =>
        val name = config.roomName.parseSingle(m)
        val exits = config.exits.parseSeq(m)
        val items = config.items.parseSeq(m)
        val occupants = config.occupants.map(_.parseSeq(m)).getOrElse(Seq.empty)
        gs = gs.copy(roomName = name, players = occupants, roomItems = items, exits = exits)
    }

    implicit val ec = context.system.dispatcher
    context.system.scheduler.schedule(1 seconds, 1000 millis, self, StressPlayer.TakeAction)
    playerManager ! PlayerManager.RegisterPlayer(self)
  }

  private def disconnect() {
    config.exitCommand().runCommand(out, in, config, gs)
    context.stop(self)
  }

  private def takeAction() {
    commandCount += 1
    if (commandCount > config.numCommandsToGive) {
      playerManager ! PlayerManager.DeregisterPlayer(self)
    } else {
      val command = config.randomValidCommand(gs)
      val startTime: Long = System.nanoTime()
      command.runCommand(out, in, config, gs) match {
        case Left(message) =>
          logResponse(startTime, System.nanoTime(), false)
          Debug.playerDebugPrint(1, "Unsuccessfull " + command.name + " command.")
          Debug.roomDebugPrint(1, gs.roomName, "Unsuccessfull " + command.name + " command in " + gs.roomName + " room.")
        case Right(state) =>
          logResponse(startTime, System.nanoTime(), true)
          Debug.playerDebugPrint(1, "Successfull " + command.name + " command.")
          Debug.roomDebugPrint(1, gs.roomName, "Successfull " + command.name + " command in " + gs.roomName + " room.")
          gs = state
      }
    }
  }

  private def logResponse(startTime: Long, stopTime: Long, res: Boolean) {
    timeKeeper ! TimeKeeper.ReceiveResponse(Response(stopTime - startTime, res))
  }
}
