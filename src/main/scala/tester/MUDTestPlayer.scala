package tester

import java.io.{PrintStream,BufferedReader}
import akka.actor.Actor
import scala.concurrent.duration._
import java.net.Socket
import scala.collection.mutable.Map
import scala.concurrent.Future

object MUDTestPlayer {
  case class GameState(roomName: String, val inventory: Seq[String], val players:Seq[String],val roomItems:Seq[String],val exits:Seq[String])
  
  case object ReadInput // MUD -> MUDTest
  case class WriteOutput(s:String) // MUDTest -> MUD
  case class TESTWriteOutput(s:String)
  case object TakeAction
  case class ChangeRoom(exit:String)
  
  def apply(n:String,s:Socket,i:BufferedReader,o:PrintStream):MUDTestPlayer = {
    new MUDTestPlayer(n,s,i,o)
  }
}

class MUDTestPlayer private(name:String,
    private val sock:Socket,
    private val in:BufferedReader,
    private val out:PrintStream) extends Actor {
  
  private var currGameState = MUDTestPlayer.GameState("", Nil, Nil, Nil, Nil)
  implicit val ec = context.system.dispatcher
  context.system.scheduler.schedule(0 seconds,100 millis,self,MUDTestPlayer.ReadInput)
  context.system.scheduler.schedule(1 seconds,1000 millis,self,MUDTestPlayer.TakeAction)
  self ! MUDTestPlayer.WriteOutput(name)
  
  // Actor Receive
  def receive = {
    case MUDTestPlayer.ReadInput => { // MUD -> MUDTest
      if(in.ready()) processInput(in.readLine())
    }
    case MUDTestPlayer.WriteOutput(s) => { // MUDTest -> MUD
      out.println(s)
    }
    case MUDTestPlayer.TakeAction => {
      if(currGameState.exits.length>0) {
        val exit = currGameState.exits(util.Random.nextInt(currGameState.exits.length))
        self ! MUDTestPlayer.ChangeRoom(exit)
      }
    }
    case MUDTestPlayer.ChangeRoom(exit) => {
      self ! MUDTestPlayer.TESTWriteOutput(exit)
      self ! MUDTestPlayer.WriteOutput(exit)
    }
    case MUDTestPlayer.TESTWriteOutput(s:String) => println(s)
    case _ =>
  }
  
  // Methods
  def processInput(input:String) {
    self ! MUDTestPlayer.TESTWriteOutput(input)
    val tokens = tokenizeInput(input)
    loadToCurrGS(tokens)
  }
  
  def tokenizeInput(input:String):Array[Array[String]] = {
    input.split('\n').map(_.split(",\\s|\\s"))
  }
  
  def loadToCurrGS(toks:Array[Array[String]]) {
    toks.foreach(line => {
      line(0) match {
        case "Players:" => currGameState = currGameState.copy(players = line.drop(1))
        case "Items:" => currGameState = currGameState.copy(roomItems = line.drop(1))
        case "Exits:" => currGameState = currGameState.copy(exits = line.drop(1))
        case _ =>
      }
    })
  }
}