package stresser.remoting

import scala.collection.mutable.Buffer
import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import utility.Response
import utility.ResponseReport

object TimeKeeper {
  case class ReceiveResponse(response: Response)
  case object SendTestReport
  case class EndStressTest(info: StressTestInfo)
  case class ChangeNumPlayers(n: Int)
  case object KillActor

  def apply(info: StressTestInfo, stressTestManager: ActorRef): TimeKeeper = {
    new TimeKeeper(info, stressTestManager)
  }
}

class TimeKeeper private (private val info: StressTestInfo,
    private val stressTestManager: ActorRef) extends Actor {
  implicit private val ec = context.system.dispatcher
  private val schedule = context.system.scheduler.schedule(1 second, 3 seconds, self, TimeKeeper.SendTestReport)

  private val responses = Buffer[Response]()
  private var numPlayers = 0

  def receive = {
    case TimeKeeper.ReceiveResponse(response) => responses += response
    case TimeKeeper.SendTestReport => {
      aggregateResponseTime() match {
        case None =>
        case Some(report) => {
          stressTestManager ! StressTestManager.ReceiveTestReport(info, report)
          responses.clear()
        }
      }
    }
    case TimeKeeper.EndStressTest(info) => {
      schedule.cancel()
      aggregateResponseTime() match {
        case None =>
        case Some(report) => {
          stressTestManager ! StressTestManager.ReceiveTestReport(info, report)
          responses.clear()
        }
      }
    }
    case TimeKeeper.ChangeNumPlayers(n) => numPlayers += n
    case TimeKeeper.KillActor => context.stop(self)
    case _ =>
  }

  private def aggregateResponseTime(): Option[ResponseReport] = {
    if (responses.isEmpty) None
    else {
      val average = responses.map(_.time).sum / responses.length
      Some(ResponseReport(average, numPlayers))
    }
  }
}