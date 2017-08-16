package tester

import java.io.PrintStream
import java.io.BufferedReader
import scala.xml.XML

/*
 * XML format stuff
 * 
 * Attributes
 * name - the command that the user would type in
 * output - specifies the type of output that should be parsed after the command is given. "room", "inventory"
 * 
 * Content tags
 * argument - these come in order and specify the type
 * 		type - exit, item, occupant, player
 */

sealed trait Command {
  def name: String
  def runCommand(out: PrintStream, in: BufferedReader, config: IOConfig, currentState: MUDTestPlayer.GameState): Either[String, MUDTestPlayer.GameState]
}

case class RoomParsing(val name: String, args: Seq[CommandArgument]) extends Command {
  def runCommand(out: PrintStream, in: BufferedReader, config: IOConfig, 
      currentState: MUDTestPlayer.GameState): Either[String, MUDTestPlayer.GameState] = {
    ???
  }
}

case class Unparsed(val name: String, args: Seq[CommandArgument]) extends Command {
  def runCommand(out: PrintStream, in: BufferedReader, config: IOConfig, 
      currentState: MUDTestPlayer.GameState): Either[String, MUDTestPlayer.GameState] = {
    ???
  }
}

case class InvParsing(val name: String, args: Seq[CommandArgument]) extends Command {
  def runCommand(out: PrintStream, in: BufferedReader, config: IOConfig, 
      currentState: MUDTestPlayer.GameState): Either[String, MUDTestPlayer.GameState] = {
    ???
  }
}
