package tester

import java.io.PrintStream
import java.io.BufferedReader
import scala.xml.XML
import scala.util.matching.Regex
import scala.annotation.tailrec

object Command {
  def sendCommand(out: PrintStream, name: String, args: Seq[CommandArgument], currentState: Player.GameState): Unit = {
    val com = name + " " + args.map(_(currentState)).mkString(" ")
    //println("Sending "+com)
    out.println(com)
  }
  def readToMatch(in: BufferedReader, regex: Regex): Either[String, Regex.Match] = {
    @tailrec
    def helper(input: String, cnt: Int): Either[String, Regex.Match] = {
      if(cnt > 1000) Left("Couldn't match room output:\n" + input)
      else {
    		val input2 = input+"\n"+in.readLine()
    		if(in.ready()) helper(input2, cnt) else {
      	  val om = regex.findFirstMatchIn(input2)
      	  if(om.isEmpty) helper(input2, cnt+1) else Right(om.get)
    		}
      }
    }
    helper("", 0)
  }
}

sealed trait Command {
	val isTerminator: Boolean
	val isMovement: Boolean
  val name: String
  val args: Seq[CommandArgument]
  def runCommand(out: PrintStream, in: BufferedReader, config: IOConfig, currentState: Player.GameState): Either[String, Player.GameState]
}

case class RoomParsing(val name: String, args: Seq[CommandArgument], isMovement: Boolean) extends Command {
	val isTerminator = false
  def runCommand(out: PrintStream, in: BufferedReader, config: IOConfig, 
      currentState: Player.GameState): Either[String, Player.GameState] = {
    Command.sendCommand(out, name, args, currentState)
    Command.readToMatch(in, config.roomOutput) match {
      case Left(message) => Left(message)
      case Right(m) =>
        val name = config.roomName.parseSingle(m)
        val exits = config.exits.parseSeq(m)
        val items = config.items.parseSeq(m)
        val occupants = config.occupants.map(_.parseSeq(m)).getOrElse(Seq.empty)
        Right(currentState.copy(roomName = name, players = occupants, roomItems = items, exits = exits))
    }
  }
}

case class Unparsed(val name: String, args: Seq[CommandArgument], isTerminator: Boolean) extends Command {
	val isMovement = false
  def runCommand(out: PrintStream, in: BufferedReader, config: IOConfig, 
      currentState: Player.GameState): Either[String, Player.GameState] = {
    Command.sendCommand(out, name, args, currentState)
    Right(currentState)
  }
}

case class InvParsing(val name: String, args: Seq[CommandArgument]) extends Command {
	val isTerminator = false
	val isMovement = false
  def runCommand(out: PrintStream, in: BufferedReader, config: IOConfig, 
      currentState: Player.GameState): Either[String, Player.GameState] = {
    Command.sendCommand(out, name, args, currentState)
    Command.readToMatch(in, config.inventoryOutput) match {
      case Left(message) => Left(message)
      case Right(m) =>
        val invItems = config.invItems.parseSeq(m)
        Right(currentState.copy(inventory = invItems))
    }
  }
}
