package tester

import java.io.{ PrintStream, BufferedReader }
import akka.actor.Actor
import scala.concurrent.duration._
import java.net.Socket
import scala.collection.mutable.Map
import scala.concurrent.Future
import akka.actor.PoisonPill
import scala.collection.mutable.Buffer
import akka.actor.ActorRef

case class Response(time:Long, result:Boolean)

object SimplePlayer {
  case object TakeAction
  
  def apply(n: String, i: BufferedReader, o: PrintStream, config: IOConfig, playerManager:ActorRef, timeKeeper:ActorRef): SimplePlayer = {
    new SimplePlayer(n, i, o, config,playerManager,timeKeeper)
  }
}
  
class SimplePlayer private (name: String,
    private val in: BufferedReader,
    private val out: PrintStream,
    private val config: IOConfig,
    private val playerManager:ActorRef,
    private val timeKeeper:ActorRef) extends Actor {

  protected var gs = Player.GameState("", Nil, Nil, Nil, Nil)
  private var commandCount = 0
  private val responses = Buffer[Response]()

  def receive() = {
    case Player.Connect => connect()
    case Player.Disconnect => disconnect()
    case SimplePlayer.TakeAction => takeAction()
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
    context.system.scheduler.schedule(1 seconds, 1000 millis, self, SimplePlayer.TakeAction)
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
      val startTime:Long = System.nanoTime()
      command.runCommand(out, in, config, gs) match {
        case Left(message) =>
          logResponse(startTime,System.nanoTime(),false)
          Debug.playerDebugPrint(1, "Unsuccessfull " + command.name + " command.")
          Debug.roomDebugPrint(1, gs.roomName, "Unsuccessfull " + command.name + " command in " + gs.roomName + " room.")
        case Right(state) =>
          logResponse(startTime,System.nanoTime(),true)
          Debug.playerDebugPrint(1, "Successfull " + command.name + " command.")
          Debug.roomDebugPrint(1, gs.roomName, "Successfull " + command.name + " command in " + gs.roomName + " room.")
          gs = state
      }
    }
  }
  
  private def logResponse(startTime:Long,stopTime:Long,res:Boolean) {
    timeKeeper ! TimeKeeper.ReceiveResponse(Response(stopTime-startTime,res))
  }
}
