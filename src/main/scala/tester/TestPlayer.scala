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

  protected var currGameState = Player.GameState("", Nil, Nil, Nil, Nil)
  private var commandCount = 0
  
  def receive() = {
    case Player.Connect => connect()
    case TestPlayer.GetDropTest => getDropTest()
    case _ =>
  }
  
  def connect() {
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
        currGameState = currGameState.copy(roomName = name, players = occupants, roomItems = items, exits = exits)
    }
  }

  def getDropTest() {
    commandCount += 1
    if (commandCount > 100) {
      println("Unsuccessful get/drop test.")
      config.exitCommand().runCommand(out, in, config, currGameState)
      context.stop(self)
    } else {
      if(currGameState.roomItems.isEmpty) {
        // move to new room and rerun
        val command = config.randomValidMovement(currGameState)
        command.runCommand(out, in, config, currGameState) match {
          case Left(message) =>
            println("Get/drop test, failed on \"" + command.name + "\" command.")
            self ! TestPlayer.GetDropTest
          case Right(state) =>
            currGameState = state
        }
        self ! TestPlayer.GetDropTest
      } else {
        // enter get/drop procedure
        val lookCommand = config.commands.filter(_.name=="look")(0)
        val invCommand = config.commands.filter(_.name=="inventory")(0)
        val getCommand = config.commands.filter(_.name=="get")(0)
        val dropCommand = config.commands.filter(_.name=="drop")(0)
        val oldInv = currGameState.inventory
        val oldRoomItems = currGameState.roomItems
        
        getCommand.runCommand(out, in, config, currGameState) match {
          case Left(message) =>
            println("Get/drop test, failed on \"" + getCommand.name + "\" command.")
            self ! TestPlayer.GetDropTest
          case Right(state) =>
            currGameState = state
        }
        lookCommand.runCommand(out, in, config, currGameState) match {
          case Left(message) =>
            println("Get/drop test, failed on \"" + lookCommand.name + "\" command.")
            self ! TestPlayer.GetDropTest
          case Right(state) =>
            currGameState = state
        }
        invCommand.runCommand(out, in, config, currGameState) match {
          case Left(message) =>
            println("Get/drop test, failed on \"" + invCommand.name + "\" command.")
            self ! TestPlayer.GetDropTest
          case Right(state) =>
            currGameState = state
        }
        
        val getInv = currGameState.inventory
        val getRoomItems = currGameState.roomItems
        if(oldRoomItems.filterNot(getRoomItems.contains(_))==getInv.filterNot(oldInv.contains(_))) {
          println("Get/drop test, successful get command.")
          
          dropCommand.runCommand(out, in, config, currGameState) match {
            case Left(message) =>
              println("Get/drop test, failed on \"" + dropCommand.name + "\" command.")
              self ! TestPlayer.GetDropTest
            case Right(state) =>
              currGameState = state
          }
          lookCommand.runCommand(out, in, config, currGameState) match {
            case Left(message) =>
              println("Get/drop test, failed on \"" + lookCommand.name + "\" command.")
              self ! TestPlayer.GetDropTest
            case Right(state) =>
              currGameState = state
          }
          invCommand.runCommand(out, in, config, currGameState) match {
            case Left(message) =>
              println("Get/drop test, failed on \"" + invCommand.name + "\" command.")
              self ! TestPlayer.GetDropTest
            case Right(state) =>
              currGameState = state
          }
          
          val dropInv = currGameState.inventory
          val dropRoomItems = currGameState.roomItems
          if(dropRoomItems.filterNot(getRoomItems.contains(_))==getInv.filterNot(dropInv.contains(_))) {
            println("Get/drop test, successful drop command.")
            
            println("Successful get/drop test.")
            config.exitCommand().runCommand(out, in, config, currGameState)
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