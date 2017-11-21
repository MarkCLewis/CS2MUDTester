package stresser.remoting

import scala.collection.mutable.Buffer
import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import utility.ResponseReport
import utility.Response
import java.io.PrintStream
import java.io.BufferedReader

object TimeKeeper {
  case object AggregateResponseTime
  case class ReceiveResponse(r: Response)
  case class DebugMessage(message: String)

  def apply(in: BufferedReader, out: PrintStream, stressTestManager: ActorRef): TimeKeeper = {
    new TimeKeeper(in, out, stressTestManager)
  }
}

class TimeKeeper private (private val in: BufferedReader,
    private val out: PrintStream,
    private val stressTestManager: ActorRef) extends Actor {

  private val responses = Buffer[Response]()

  implicit private val ec = context.system.dispatcher
  context.system.scheduler.schedule(1 seconds, 1 seconds, self, TimeKeeper.AggregateResponseTime)

  def receive = {
    case TimeKeeper.AggregateResponseTime => {
      aggregateResponseTime() match {
        case None =>
        case Some(r) => {
          out.println("responses: " + r.numResponses + ", average: " + r.average / 1000000.0 + " ms.")
          responses.clear()
        }
      }
    }
    case TimeKeeper.ReceiveResponse(r) => responses += r
    case TimeKeeper.DebugMessage(message) => out.println(message)
    case _ =>
  }

  private def aggregateResponseTime(): Option[ResponseReport] = {
    if (responses.isEmpty) None
    else {
      val average = responses.map(_.time).sum / responses.length
      val numResponses = responses.length
      responses.clear()
      Some(ResponseReport(average, numResponses))
    }
  }
}