package stresser.remoting

import java.io.BufferedReader
import java.io.PrintStream
import java.net.Socket

import scala.collection.mutable.Buffer
import scala.collection.mutable.Queue
import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Address
import akka.actor.Deploy
import akka.actor.Props
import akka.remote.RemoteScope
import utility.IOConfig
import utility.ResponseReport

case class StressTestInfo(sock: Socket, in: BufferedReader, out: PrintStream, host: String, port: Int, ioConfig: IOConfig)

object StressTestManager {
  case class EnqueueStressTestInfo(info: StressTestInfo)
  case object RunNextStressTest
  case class EndStressTest(info: StressTestInfo, timeKeeper: ActorRef, playerManagers: Array[ActorRef])
  case class ReceiveTestReport(info: StressTestInfo, report: ResponseReport)

  def apply(): StressTestManager = {
    new StressTestManager()
  }
}

class StressTestManager private () extends Actor {
  implicit private val ec = context.system.dispatcher
  context.system.scheduler.schedule(1 second, 5 seconds, self, StressTestManager.RunNextStressTest)

  private val nodes = "0 1 2 3 4 5 6 7 8".split(" ").map("131.194.71.13" + _.trim)
  private var testing = false
  private val testDuration = 120 seconds

  private val tests = Queue[StressTestInfo]()
  private val reports = Buffer[ResponseReport]()

  def receive = {
    case StressTestManager.EnqueueStressTestInfo(info) => {
      tests.enqueue(info)
      if (tests.length > 1) info.out.println("Test enqueued.\n" + (tests.length - 1) + " tests ahead.")
    }
    case StressTestManager.RunNextStressTest => {
      if (tests.size > 0 && !testing) {
        testing = true
        stressTest(tests.dequeue())
      }
    }
    case StressTestManager.EndStressTest(info, timeKeeper, playerManagers) => {
      playerManagers.foreach(_ ! StressPlayerManager.EndStressTest)
      timeKeeper ! TimeKeeper.EndStressTest(info)
      playerManagers.foreach(_ ! StressPlayerManager.KillActor)
      timeKeeper ! TimeKeeper.KillActor
      info.sock.close()
      info.out.println("End stress test.")
      testing = false
    }
    case StressTestManager.ReceiveTestReport(info, report) => {
      reports += report
      info.out.println(report.numPlayers + " actors, average response time: " + report.average / 1000000.0 + " ms.")
    }
    case _ =>
  }

  private def stressTest(info: StressTestInfo) {
    reports.clear()
    info.out.println("Running stress test.\nRunning for " + testDuration + ".")
    val timeKeeper = context.system.actorOf(Props(TimeKeeper(info, self)), "TimeKeeper")
    val playerManagers = nodes.map { ip =>
      val address = Address("akka.tcp", "MUDStressTest", ip, 5150)
      context.system.actorOf(Props(StressPlayerManager(info, timeKeeper, ip)).withDeploy(Deploy(scope = RemoteScope(address))))
    }
    context.system.scheduler.scheduleOnce(testDuration, self, StressTestManager.EndStressTest(info, timeKeeper, playerManagers))
  }

  private def assemblePlot(info: StressTestInfo) {

  }
}