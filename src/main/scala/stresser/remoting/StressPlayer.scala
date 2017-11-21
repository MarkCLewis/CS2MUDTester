package stresser.remoting

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
import utility.ResponseReport
import utility.Response

object StressPlayer {
  case object TakeAction

  def apply(name: String,
      in: BufferedReader,
      out: PrintStream,
      config: IOConfig,
      playerManager: ActorRef,
      timeKeeper: ActorRef): StressPlayer = {
    new StressPlayer(name, in, out, config, playerManager, timeKeeper)
  }
}

class StressPlayer private (private val name: String,
    private val in: BufferedReader,
    private val out: PrintStream,
    private val config: IOConfig,
    private val playerManager: ActorRef,
    private val timeKeeper: ActorRef) extends Actor {

  implicit val ec = context.system.dispatcher
  protected var gs = Player.GameState("", Nil, Nil, Nil, Nil)
  private var commandCount = 0
  private val responses = Buffer[Response]()
  private var dead = false
  
  def receive() = {
    case Player.Connect => {
      connect()
      implicit val ec = context.system.dispatcher
      context.system.scheduler.schedule(1 seconds, 1 second, self, StressPlayer.TakeAction)
    }
    case Player.KillPlayer => disconnect()
    case StressPlayer.TakeAction => {
      if (!dead) takeAction() match {
        case None => disconnect()
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

  private def disconnect() {
    dead = true
    config.exitCommand().runCommand(out, in, config, gs)
    playerManager ! PlayerManager.DeregisterPlayer(self)
  }

  private def takeAction(): Option[Response] = {
    //commandCount += 1
    //if (commandCount > config.numCommandsToGive) {
    //println(name + " out of commands.")
    //None
    //}
    //else {
    // TODO should StressPlayers just move around or perform other commands as well?
    val command = config.randomValidMovement(gs)
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
    //}
  }
}
