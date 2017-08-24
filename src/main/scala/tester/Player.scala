package tester

import java.io.{PrintStream,BufferedReader}
import akka.actor.Actor

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
  
  def connect()
  def takeAction()
  
}