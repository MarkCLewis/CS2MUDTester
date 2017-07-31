package tester

import java.io.{PrintStream,BufferedReader}
import akka.actor.Actor
import scala.concurrent.duration._
import java.net.Socket
import scala.collection.mutable.Map
import scala.concurrent.Future

object MUDTestPlayer {
  case class GameState(var players:Array[String],var items:Array[String],var exits:Array[String])
  
  case object ReadInput // MUD -> MUDTest
  case class WriteOutput(s:String) // MUDTest -> MUD
  case class TESTWriteOutput(s:String)
  case object ChangeRoom
  
  def apply(n:String,s:Socket,i:BufferedReader,o:PrintStream):MUDTestPlayer = {
    new MUDTestPlayer(n,s,i,o)
  }
}

class MUDTestPlayer private(name:String,
    private val sock:Socket,
    private val in:BufferedReader,
    private val out:PrintStream) extends Actor {
  
  val currGameState = MUDTestPlayer.GameState(Array[String](),Array[String](),Array[String]())
  implicit val ec = context.system.dispatcher
  context.system.scheduler.schedule(0 seconds,100 millis,self,MUDTestPlayer.ReadInput)
  context.system.scheduler.schedule(1 seconds,1000 millis,self,MUDTestPlayer.ChangeRoom)
  self ! MUDTestPlayer.WriteOutput(name)
  
  // Actor Receive
  def receive = {
    case MUDTestPlayer.ReadInput => { // MUD -> MUDTest
      if(in.ready()) processInput(in.readLine())
    }
    case MUDTestPlayer.WriteOutput(s:String) => { // MUDTest -> MUD
      out.println(s)
    }
    case MUDTestPlayer.ChangeRoom => {
      if(currGameState.exits.length>0) {
        val exit = currGameState.exits(util.Random.nextInt(currGameState.exits.length))
        self ! MUDTestPlayer.TESTWriteOutput(exit)
        self ! MUDTestPlayer.WriteOutput(exit)
      }
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
        case "Players:" => currGameState.players = line.drop(1)
        case "Items:" => currGameState.items = line.drop(1)
        case "Exits:" => currGameState.exits = line.drop(1)
        case _ =>
      }
    })
  }
}