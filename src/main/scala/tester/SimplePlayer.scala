package tester

import java.io.{ PrintStream, BufferedReader }
import akka.actor.Actor
import scala.concurrent.duration._
import java.net.Socket
import scala.collection.mutable.Map
import scala.concurrent.Future
import akka.actor.PoisonPill

object SimplePlayer {
  case object TakeAction
  
  def apply(n: String, i: BufferedReader, o: PrintStream, config: IOConfig): SimplePlayer = {
    new SimplePlayer(n, i, o, config)
  }
}
  
class SimplePlayer private (name: String,
    private val in: BufferedReader,
    private val out: PrintStream,
    val config: IOConfig) extends Actor {

  protected var currGameState = Player.GameState("", Nil, Nil, Nil, Nil)
  private var commandCount = 0

  def receive() = {
    case Player.Connect => connect()
    case SimplePlayer.TakeAction => takeAction()
    case _ =>
  }
  
  def connect() {
    // Tell name for login
    out.println(name)
    
    // Read initial room descriptionc
    Command.readToMatch(in, config.roomOutput) match {
      case Left(message) => println(message)
      case Right(m) =>
        val name = config.roomName.parseSingle(m)
        val exits = config.exits.parseSeq(m)
        val items = config.items.parseSeq(m)
        val occupants = config.occupants.map(_.parseSeq(m)).getOrElse(Seq.empty)
        currGameState = currGameState.copy(roomName = name, players = occupants, roomItems = items, exits = exits)
    }
    
    implicit val ec = context.system.dispatcher
    context.system.scheduler.schedule(1 seconds, 1000 millis, self, SimplePlayer.TakeAction)
  }
  
  def takeAction() {
    //println("Issuing command " + commandCount)
    commandCount += 1
    if (commandCount > config.numCommandsToGive) {
      config.exitCommand().runCommand(out, in, config, currGameState)
      context.stop(self)
    } else {
      val command = config.randomValidCommand(currGameState)
      //println(command)
      command.runCommand(out, in, config, currGameState) match {
        case Left(message) =>
          Debug.playerDebugPrint(1, "Unsuccessfull " + command.name + " command.")
          Debug.roomDebugPrint(1, currGameState.roomName, "Unsuccessfull " + command.name + " command in " + currGameState.roomName + " room.")
        case Right(state) =>
          Debug.playerDebugPrint(1, "Successfull " + command.name + " command.")
          Debug.roomDebugPrint(1, currGameState.roomName, "Successfull " + command.name + " command in " + currGameState.roomName + " room.")
          currGameState = state
        //println(currGameState)
      }
    }
  }

}