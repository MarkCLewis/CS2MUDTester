package tester

import scala.xml.XML
import java.io.PrintStream
import java.io.BufferedReader

class LookCommand(cN:String,cT:String,cE:Boolean,cAs:Array[String]) extends Command_ {
  val cName = cN
  val cType = cT
  val cEnabled = cE
  val cArguments = cAs
  def runCommand(out:PrintStream,in:BufferedReader,gs:MUDTestPlayer.GameState):Either[String,MUDTestPlayer.GameState] = {
    val command = buildCommand(gs)
    // print command
    // print look command
    // read input
    // build new game state
    Right(gs)
  }
}

class InventoryCommand(cN:String,cT:String,cE:Boolean,cAs:Array[String]) extends Command_ {
  val cName = cN
  val cType = cT
  val cEnabled = cE
  val cArguments = cAs
  def runCommand(out:PrintStream,in:BufferedReader,gs:MUDTestPlayer.GameState):Either[String,MUDTestPlayer.GameState] = {
    val command = buildCommand(gs)
    // print command
    // print inventory command
    // read input
    // build new game state
    Right(gs)
  }
}

class SimpleCommand(cN:String,cT:String,cE:Boolean,cAs:Array[String]) extends Command_ {
  val cName = cN
  val cType = cT
  val cEnabled = cE
  val cArguments = cAs
  def runCommand(out:PrintStream,in:BufferedReader,gs:MUDTestPlayer.GameState):Either[String,MUDTestPlayer.GameState] = {
    val command = buildCommand(gs)
    // print command
    Right(gs)
  }
}

object Config {
  
  def apply(file:String) = {
    val root = (XML.loadFile(file) \ "command")
    
    val allCommands = root.map(node => {
      val cN = (node \ "@name").text.trim()
      val cT = (node \ "@type").text.trim()
      val cE = (node \ "@enabled").text.trim().toBoolean
      val cAs = ((node \ "arguments") \ "argument").map(arg => {
        ((arg \ "@number").text.trim().toInt,(arg \ "@type").text.trim())
      }).sortWith(_._1 < _._1).map(_._2).asInstanceOf[Array[String]]
      
      cT match {
        case "direction" => new LookCommand(cN,cT,cE,cAs)
      }
    })
    val enabledCommands = allCommands.filter(_.cEnabled)
    val dirCommands = enabledCommands.filter(_.cType == "direction")
    
    new Config(dirCommands)
  }
}

class Config private(val dirCommands:Seq[Command_]) {
  
}