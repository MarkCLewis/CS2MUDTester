package tester

import java.io.{ PrintStream, BufferedReader }
import akka.actor.Actor

object TestPlayer {
  case object GetDropTest
  
  def apply(n: String, i: BufferedReader, o: PrintStream, c: IOConfig): TestPlayer = {
    new TestPlayer(n, i, o, c)
  }
}

class TestPlayer private (name: String,
    private val in: BufferedReader,
    private val out: PrintStream,
    val config: IOConfig) extends Actor {

  protected var gs = Player.GameState("", Nil, Nil, Nil, Nil)
  private var commandCount = 0
  
  def receive() = {
    case Player.Connect => connect()
    case TestPlayer.GetDropTest => getDropTest()
    case _ =>
  }
  
  private def connect() {
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
        gs = gs.copy(roomName = name, players = occupants, roomItems = items, exits = exits)
    }
  }

  private def getDropTest() {
    commandCount += 1
    if (commandCount > 1000) {
      println("Unsuccessful get/drop test.")
      config.exitCommand().runCommand(out, in, config, gs)
      context.stop(self)
    } else {
      if(gs.roomItems.isEmpty) {
        // move to new room and rerun
        val command = config.randomValidMovement(gs)
        command.runCommand(out, in, config, gs) match {
          case Left(message) =>
            println("Get/drop test, failed on \"" + command.name + "\" command.")
            self ! TestPlayer.GetDropTest
          case Right(state) =>
            gs = state
        }
        self ! TestPlayer.GetDropTest
      } else {
        // enter get/drop procedure
        val lookCommand = config.commands.filter(_.name=="look")(0)
        val invCommand = config.commands.filter(_.name=="inventory")(0)
        val getCommand = config.commands.filter(_.name=="get")(0)
        val dropCommand = config.commands.filter(_.name=="drop")(0)
        val oldInv = gs.inventory
        val oldRoomItems = gs.roomItems
        
        getCommand.runCommand(out, in, config, gs) match {
          case Left(message) =>
            println("Get/drop test, failed on \"" + getCommand.name + "\" command.")
            self ! TestPlayer.GetDropTest
          case Right(state) =>
            gs = state
        }
        lookCommand.runCommand(out, in, config, gs) match {
          case Left(message) =>
            println("Get/drop test, failed on \"" + lookCommand.name + "\" command.")
            self ! TestPlayer.GetDropTest
          case Right(state) =>
            gs = state
        }
        invCommand.runCommand(out, in, config, gs) match {
          case Left(message) =>
            println("Get/drop test, failed on \"" + invCommand.name + "\" command.")
            self ! TestPlayer.GetDropTest
          case Right(state) =>
            gs = state
        }
        
        val getInv = gs.inventory
        val getRoomItems = gs.roomItems
        if(oldRoomItems.filterNot(getRoomItems.contains(_))==getInv.filterNot(oldInv.contains(_))) {
          println("Get/drop test, successful get command.")
          
          dropCommand.runCommand(out, in, config, gs) match {
            case Left(message) =>
              println("Get/drop test, failed on \"" + dropCommand.name + "\" command.")
              self ! TestPlayer.GetDropTest
            case Right(state) =>
              gs = state
          }
          lookCommand.runCommand(out, in, config, gs) match {
            case Left(message) =>
              println("Get/drop test, failed on \"" + lookCommand.name + "\" command.")
              self ! TestPlayer.GetDropTest
            case Right(state) =>
              gs = state
          }
          invCommand.runCommand(out, in, config, gs) match {
            case Left(message) =>
              println("Get/drop test, failed on \"" + invCommand.name + "\" command.")
              self ! TestPlayer.GetDropTest
            case Right(state) =>
              gs = state
          }
          
          val dropInv = gs.inventory
          val dropRoomItems = gs.roomItems
          if(dropRoomItems.filterNot(getRoomItems.contains(_))==getInv.filterNot(dropInv.contains(_))) {
            println("Get/drop test, successful drop command.")
            
            println("Successful get/drop test.")
            config.exitCommand().runCommand(out, in, config, gs)
            context.stop(self)
          } else {
            println("Get/drop test, unsuccessful drop command.")
            self ! TestPlayer.GetDropTest
          }
        } else {
          println("Get/drop test, unsuccessful get command.")
          self ! TestPlayer.GetDropTest
        }
        
      }
    }
  }
  
}