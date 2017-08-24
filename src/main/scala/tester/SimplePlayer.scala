package tester

import java.io.{ PrintStream, BufferedReader }
import akka.actor.Actor
import scala.concurrent.duration._
import java.net.Socket
import scala.collection.mutable.Map
import scala.concurrent.Future
import akka.actor.PoisonPill

object SimplePlayer {
  def apply(n: String, i: BufferedReader, o: PrintStream, config: IOConfig): SimplePlayer = {
    new SimplePlayer(n, i, o, config)
  }
}

class SimplePlayer private (name: String,
    private val in: BufferedReader,
    private val out: PrintStream,
    val config: IOConfig) extends Player(name,in,out,config) {
  
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
          Debug.playerDebugPrint(1,"Unsuccessfull " + command.name + " command.")
          Debug.roomDebugPrint(1,currGameState.roomName,"Unsuccessfull " + command.name + " command in " + currGameState.roomName + " room.")
        case Right(state) =>
          Debug.playerDebugPrint(1,"Successfull " + command.name + " command.")
          Debug.roomDebugPrint(1,currGameState.roomName,"Successfull " + command.name + " command in " + currGameState.roomName + " room.")
          currGameState = state
          //println(currGameState)
      }
    }
  }

}