package tester

import java.io.PrintStream
import java.io.BufferedReader
import scala.xml.XML

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
