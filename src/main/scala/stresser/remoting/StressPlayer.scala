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
import utility.Response
import akka.actor.Cancellable
import utility.ActorMessages
import com.typesafe.config.ConfigFactory

object StressPlayer {
  case object TakeAction

  def apply(name: String,
    in: BufferedReader,
    out: PrintStream,
    config: IOConfig,
    timeKeeper: ActorRef): StressPlayer = {
    new StressPlayer(name, in, out, config, timeKeeper)
  }
}

class StressPlayer private (private val name: String,
    private val in: BufferedReader,
    private val out: PrintStream,
    private val ioConfig: IOConfig,
    private val timeKeeper: ActorRef) extends Actor {
  implicit val ec = context.system.dispatcher

  protected var gs = Player.GameState("", Nil, Nil, Nil, Nil)
  private var commandCount = 0
  private val responses = Buffer[Response]()
  private var dead = false
  private var schedule: Cancellable = _

  def receive() = {
    case Player.Connect => {
      connect()
      implicit val ec = context.system.dispatcher
      schedule = context.system.scheduler.schedule(1 seconds, 500 millis, self, StressPlayer.TakeAction)
    }
    case StressPlayer.TakeAction => {
      if (!dead) takeAction() match {
        case None => disconnect()
        case Some(r) => {
          if (r.result && util.Random.nextInt(500) < 1) timeKeeper ! TimeKeeper.ReceiveResponse(r)
        }
      }
    }
    case ActorMessages.EndStressTest => disconnect()
    case _ =>
  }

  private def connect() {
    in.readLine()
    out.println(name)
    Command.readToMatch(in, ioConfig.roomOutput) match {
      case Left(message) => println(message)
      case Right(m) =>
        val name = ioConfig.roomName.parseSingle(m)
        val exits = ioConfig.exits.parseSeq(m)
        val items = ioConfig.items.parseSeq(m)
        val occupants = ioConfig.occupants.map(_.parseSeq(m)).getOrElse(Seq.empty)
        gs = gs.copy(roomName = name, players = occupants, roomItems = items, exits = exits)
    }
  }

  private def disconnect() {
    dead = true
    ioConfig.exitCommand().runCommand(out, in, ioConfig, gs)
    context.stop(self)
  }

  private def takeAction(): Option[Response] = {
    //commandCount += 1
    //if (commandCount > config.numCommandsToGive) {
    //println(name + " out of commands.")
    //None
    //}
    //else {
    // TODO should StressPlayers just move around or perform other commands as well?
    try {
      val command = ioConfig.randomValidMovement(gs)
      val startTime: Long = System.nanoTime()
      command.runCommand(out, in, ioConfig, gs) match {
        case Left(message) =>
          timeKeeper ! TimeKeeper.DebugMessage(message)
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
    } catch {
      case e: java.net.SocketException => {
        timeKeeper ! ActorMessages.EmergencyShutdown(None)
        context.stop(self)
        None
      }
    }
  }
}
