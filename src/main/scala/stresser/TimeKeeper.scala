package stresser

import akka.actor.Actor
import scala.collection.mutable.Buffer
import scala.concurrent.duration._
import akka.actor.ActorRef

object TimeKeeper {
  case object AggregateResponseTime
  case class ReceiveResponse(r: Response)

  def apply(playerManager:ActorRef): TimeKeeper = {
    new TimeKeeper(playerManager)
  }
}

class TimeKeeper private (private val playerManager:ActorRef) extends Actor {

  private val responses = Buffer[Response]()

  implicit private val ec = context.system.dispatcher
  context.system.scheduler.schedule(1 seconds, 1 seconds, self, TimeKeeper.AggregateResponseTime)

  def receive = {
    case TimeKeeper.AggregateResponseTime => {
      aggregateResponseTime() match {
        case None =>
        case Some(r) => playerManager ! StressPlayerManager.ReceiveResponseReport(r)
      }
    }
    case TimeKeeper.ReceiveResponse(r) =>  responses += r
    case _ =>
  }

  private def aggregateResponseTime():Option[ResponseReport] = {
    if (responses.isEmpty) None
    else {
      val average = responses.map(_.time).sum / responses.length
      val numResponses = responses.length
      responses.clear()
      Some(ResponseReport(average,numResponses))
    }
  }
}