package tester

import java.io.{PrintStream,BufferedReader}
import akka.actor.Actor
import scala.concurrent.duration._

object Player {
  case class GameState(roomName: String, val inventory: Seq[String], val players: Seq[String], val roomItems: Seq[String], val exits: Seq[String])

  case object Connect
  case object TakeAction
}

abstract class Player (name:String,
    in:BufferedReader,
    out:PrintStream,
    config:IOConfig) extends Actor {
  
  protected var currGameState = Player.GameState("", Nil, Nil, Nil, Nil)
  protected var commandCount = 0
  
  final def receive = {
    case Player.Connect => connect()
    case Player.TakeAction => takeAction()
    case _ =>
  }
  
  final def connect() {
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
        currGameState = currGameState.copy(roomName = name, players = occupants, roomItems = items, exits = exits)
    }
    //println(currGameState)
    
    implicit val ec = context.system.dispatcher
    context.system.scheduler.schedule(1 seconds, 1000 millis, self, Player.TakeAction)
  }
    
  def takeAction()
  
}