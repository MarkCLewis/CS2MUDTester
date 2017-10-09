package tester

import java.io.{PrintStream,BufferedReader}
import akka.actor.Actor
import scala.concurrent.duration._

object Player {
  case class GameState(roomName: String, val inventory: Seq[String], val players: Seq[String], val roomItems: Seq[String], val exits: Seq[String])

  case object Connect
  case object Disconnect
}