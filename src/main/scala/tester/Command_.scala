package tester

import java.io.{PrintStream,BufferedReader}

abstract class Command_ {
  val cName:String
  val cType:String
  val cEnabled:Boolean
  val cArguments:Array[String]
  def runCommand(out:PrintStream,in:BufferedReader,gs:MUDTestPlayer.GameState):Either[String,MUDTestPlayer.GameState]
  
  final def buildCommand(gs:MUDTestPlayer.GameState):String = {
    cName + " " + cArguments.map(arg => {
      arg match {
        case "item" => gs.items(util.Random.nextInt(gs.items.length))
        case "player" => gs.players(util.Random.nextInt(gs.players.length))
        case "message" => "Hello World!"
      }
    }).mkString(" ")
  }
}