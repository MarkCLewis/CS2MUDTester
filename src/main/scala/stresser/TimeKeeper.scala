package stresser

import akka.actor.Actor
import scala.collection.mutable.Buffer
import scala.concurrent.duration._

object TimeKeeper {
  case object AggregateResponseTime
  case class ReceiveResponse(response:Response)
  // TODO better way to grab player number w/o having to send a message to player manager
  case object PlayerRegistered
  
  def apply():TimeKeeper = {
    new TimeKeeper()
  }
}

class TimeKeeper private() extends Actor {
  
  private val responses = Buffer[Response]()
  private var numPlayers = 0
  
  implicit private val ec = context.system.dispatcher
  context.system.scheduler.schedule(1 seconds, 1 seconds, self, TimeKeeper.AggregateResponseTime)
  
  def receive = {
    case TimeKeeper.AggregateResponseTime => aggregateResponseTime()
    case TimeKeeper.ReceiveResponse(response) => responses += response
    case TimeKeeper.PlayerRegistered => numPlayers+=1
    case _ =>
  }
  
  private def aggregateResponseTime() {
    if(!responses.isEmpty) {
      val average = responses.map(_.time).sum/responses.length
      println("Number of Players: " + numPlayers + ", number of commands: " + responses.length + ", average response time: " + average/1000000.0 + " ms")
      responses.clear()
    }
  }
}