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
case class ResponseReport(average: Long, numResponses: Int)

object StressPlayer {
  case object TakeAction
  case class IncreasePlayerCount(n: Int)
  case class DecreasePlayerCount(n: Int)

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

  implicit val ec = context.system.dispatcher
  protected var gs = Player.GameState("", Nil, Nil, Nil, Nil)
  private var commandCount = 0
  private val responses = Buffer[Response]()
  private var playerCount = 1
  private var dead = false

  def receive() = {
    case Player.Connect => {
      connect()
      implicit val ec = context.system.dispatcher
      context.system.scheduler.schedule(1 seconds, 1 second, self, StressPlayer.TakeAction)
    }
    case StressPlayer.TakeAction => {
      if (!dead) takeAction() match {
        case None => {
          dead = true
          config.exitCommand().runCommand(out, in, config, gs)
          playerManager ! PlayerManager.DeregisterPlayer(self)
        }
        case Some(r) => {
          if (r.result && util.Random.nextInt(100) < 1) {
            timeKeeper ! TimeKeeper.ReceiveResponse(r)
          }
        }
      }
    }
    case _ =>
  }

  private def connect() {
    out.println(name)
    Command.readToMatch(in, config.roomOutput) match {
      case Left(message) => println(message)
      case Right(m) =>
        val name = config.roomName.parseSingle(m)
        val exits = config.exits.parseSeq(m)
        val items = config.items.parseSeq(m)
        val occupants = config.occupants.map(_.parseSeq(m)).getOrElse(Seq.empty)
        gs = gs.copy(roomName = name, players = occupants, roomItems = items, exits = exits)
    }
  }

  private def takeAction(): Option[Response] = {
    commandCount += 1
    if (commandCount > config.numCommandsToGive) None
    else {
      val command = config.randomValidCommand(gs)
      val startTime: Long = System.nanoTime()
      command.runCommand(out, in, config, gs) match {
        case Left(message) =>
          val stopTime: Long = System.nanoTime()
          Debug.playerDebugPrint(1, "Unsuccessfull " + command.name + " command.")
          Debug.roomDebugPrint(1, gs.roomName, "Unsuccessfull " + command.name + " command in " + gs.roomName + " room.")
          Some(Response(stopTime - startTime, false))
        //None
        case Right(state) =>
          val stopTime: Long = System.nanoTime()
          Debug.playerDebugPrint(1, "Successfull " + command.name + " command.")
          Debug.roomDebugPrint(1, gs.roomName, "Successfull " + command.name + " command in " + gs.roomName + " room.")
          gs = state
          Some(Response(stopTime - startTime, true))
      }
    }
  }
}
