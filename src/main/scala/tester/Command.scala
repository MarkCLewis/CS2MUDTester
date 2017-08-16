package tester

import java.io.PrintStream
import java.io.BufferedReader
import scala.xml.XML

object Command {
  def readConfig(flags: Map[String,Option[String]]): Seq[Command] = {
    val configFile = if(flags.contains("-config")) {
      flags("-config") match {
        case Some(value) => value
        case None => {
          println("No -config specified, defaulting to config.xml.")
          "config.xml"
        }
      }
    } else {
      println("No -config specified, defaulting to config.xml.")
      "config.xml"
    }
    val xml = XML.loadFile(configFile)
    (xml \ "command").flatMap { n =>
      val enabled = (n \ "@enabled").text
      if(enabled=="true") {
        val name = (n \ "@name").text
        val ctype = (n \ "@type").text
        ctype match {
          case "direction" => Seq(Direction(name))
          case "look" => ???
          case "get" => ???
          case "drop" => ???
          case "inventory" => ???
          case _ => Nil
        }
      } else {
        Nil
      }
    }
  }
}

sealed trait Command {
  def name: String
  def runCommand(out: PrintStream, in: BufferedReader, currentState: MUDTestPlayer.GameState): Either[String, MUDTestPlayer.GameState]
}

case class Direction(val name: String) extends Command {
  def runCommand(out: PrintStream, in: BufferedReader, 
      currentState: MUDTestPlayer.GameState): Either[String, MUDTestPlayer.GameState] = {
    ???
  }
}