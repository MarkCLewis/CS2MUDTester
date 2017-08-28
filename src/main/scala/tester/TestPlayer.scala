package tester

import java.io.{ PrintStream, BufferedReader }
import akka.actor.Actor

object TestPlayer {  
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

}