package tester

import java.io.BufferedReader
import java.io.PrintStream

import akka.actor.Actor
import utility.Command
import utility.IOConfig
import utility.Player
import utility.PlayerManager

object TestPlayer {
  case object GetDropTest extends Test {
    override def toString(): String = "get/drop"
  }

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
    case Player.Connect => {
      connect()
      sender ! PlayerManager.RegisterPlayer
    }
    case Player.Disconnect => {
      config.exitCommand().runCommand(out, in, config, gs)
      context.stop(self)
    }
    case TestPlayer.GetDropTest => {
      getDropTest() match {
        case Left(command) => sender ! TestPlayerManager.NegativeTestResult(TestPlayer.GetDropTest, command)
        case Right(b) => sender ! TestPlayerManager.PositiveTestResult(TestPlayer.GetDropTest)
      }
      sender ! PlayerManager.DeregisterPlayer
    }
    case _ =>
  }

  private def connect() {
    out.println(name)
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

  private def getDropTest(): Either[Command, Boolean] = {
    /*
     * Procedure:
     * 
     * 1. Run around until an item is found.
     * 2. Grab item.
     * 3. Check item is in inventory and no longer in room.
     * 4. Drop item.
     * 5. Check item is in room and no longer in inventory.
     * 
     */

    println("Running get/drop procedure.")
    val lookCommand = config.commands.filter(_.name == "look")(0)
    val invCommand = config.commands.filter(_.name == "inventory")(0)
    val getCommand = config.commands.filter(_.name == "get")(0)
    val dropCommand = config.commands.filter(_.name == "drop")(0)

    // 1. Run around until an item is found.
    while (gs.roomItems.isEmpty) {
      if (commandCount > 1000) {
        return Left(lookCommand)
      } else {
        val command = config.randomValidMovement(gs)
        command.runCommand(out, in, config, gs) match {
          case Left(message) => return Left(command)
          case Right(state) => gs = state
        }
        commandCount += 1
      }
    }

    val oldInv = gs.inventory
    val oldRoomItems = gs.roomItems

    // 2. Grab item.
    getCommand.runCommand(out, in, config, gs) match {
      case Left(message) => return Left(getCommand)
      case Right(state) => gs = state
    }

    // 3. Check item is in inventory and no longer in room.
    lookCommand.runCommand(out, in, config, gs) match {
      case Left(message) => return Left(lookCommand)
      case Right(state) => gs = state
    }
    invCommand.runCommand(out, in, config, gs) match {
      case Left(message) => return Left(invCommand)
      case Right(state) => gs = state
    }
    val getInv = gs.inventory
    val getRoomItems = gs.roomItems
    if (oldRoomItems.filterNot(getRoomItems.contains(_)) == getInv.filterNot(oldInv.contains(_))) {

      // 4. Drop item.
      dropCommand.runCommand(out, in, config, gs) match {
        case Left(message) => return Left(dropCommand)
        case Right(state) => gs = state
      }

      // 5. Check item is in room and no longer in inventory.
      lookCommand.runCommand(out, in, config, gs) match {
        case Left(message) => return Left(lookCommand)
        case Right(state) => gs = state
      }
      invCommand.runCommand(out, in, config, gs) match {
        case Left(message) => return Left(invCommand)
        case Right(state) => gs = state
      }
      val dropInv = gs.inventory
      val dropRoomItems = gs.roomItems
      if (dropRoomItems.filterNot(getRoomItems.contains(_)) == getInv.filterNot(dropInv.contains(_))) {
        return Right(true)
      } else {
        return Left(dropCommand)
      }

    } else {
      return Left(getCommand)
    }
  }

}