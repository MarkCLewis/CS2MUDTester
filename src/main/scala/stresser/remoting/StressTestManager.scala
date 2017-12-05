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

import swiftvis2.plotting.Plot
import swiftvis2.plotting.renderer.SVGRenderer
import swiftvis2.plotting.PlotDoubleSeries
import utility.ActorMessages

import com.typesafe.config.ConfigFactory

case class StressTestInfo(sock: Socket, in: BufferedReader, out: PrintStream, host: String, port: Int, ioConfig: IOConfig, outputFile: String)

object StressTestManager {
  case class EnqueueStressTestInfo(info: StressTestInfo)
  case object RunNextStressTest
  case class EndStressTest(info: StressTestInfo, timeKeeper: ActorRef, playerManagers: Array[ActorRef])
  case class ReceiveTestReport(info: StressTestInfo, report: ResponseReport)
  case class GenerateReportPlot(info: StressTestInfo, report: Option[ResponseReport])

  def apply(): StressTestManager = {
    new StressTestManager()
  }
}

class StressTestManager private () extends Actor {
  implicit private val ec = context.system.dispatcher //s.lookup("prio-dispatcher")
  context.system.scheduler.schedule(1 second, 5 seconds, self, StressTestManager.RunNextStressTest)

  private val nodes = "0 1 2 3 4 5 6 7 8".split(" ").map("131.194.71.13" + _.trim)
  private var testing = false
  private val testDuration = 120 seconds

  private val tests = Queue[StressTestInfo]()
  private val reports = Buffer[ResponseReport]()

  def receive = {
    case StressTestManager.EnqueueStressTestInfo(info) => {
      tests.enqueue(info)
      /*if (tests.length > 1)*/ info.out.println("Test enqueued.\n" + (tests.length - 1) + " tests ahead.")
    }
    case StressTestManager.RunNextStressTest => {
      if (tests.size > 0 && !testing) {
        testing = true
        stressTest(tests.dequeue())
      }
    }
    case StressTestManager.EndStressTest(info, timeKeeper, playerManagers) => {
      timeKeeper ! ActorMessages.EndStressTest(info)
      playerManagers.foreach(_ ! ActorMessages.EndStressTest)
    }
    case StressTestManager.ReceiveTestReport(info, report) => {
      enqueueReport(info, report)
    }
    case StressTestManager.GenerateReportPlot(info, report) => {
      report match {
        case Some(r) => enqueueReport(info, r)
        case None =>
      }
      generatePlotReport(info)
      info.out.println("End stress test.")
      info.out.println("To display results plot, enter: ssh -Y pandora00 \"display " + info.outputFile + "\"")
      info.sock.close()
      reports.clear()
      testing = false
    }
    case ActorMessages.EmergencyShutdown(info) => {
      context.children.foreach { child =>
        if (child.compareTo(self) != 0) context.stop(child)
      }
      info.out.println("Emergency shutdown.")
      info.sock.close()
      reports.clear()
      testing = false
    }
    case _ => 
  }

  private def stressTest(info: StressTestInfo) {
    reports.clear()
    val sock = new Socket(info.host, info.port)
    info.out.println("Running stress test.\nRunning for " + testDuration + ".")
    val timeKeeper = context.system.actorOf(Props(TimeKeeper(info, self)))
    val playerManagers = nodes.map { ip =>
      val address = Address("akka.tcp", "MUDStressTest", ip, 5150)
      context.system.actorOf(Props(StressPlayerManager(info, self, timeKeeper, ip))
        .withDeploy(Deploy(scope = RemoteScope(address))))
    }
    context.system.scheduler.scheduleOnce(testDuration, self, StressTestManager.EndStressTest(info, timeKeeper, playerManagers))
  }

  private def enqueueReport(info: StressTestInfo, report: ResponseReport) {
    reports += report
    info.out.println(report.numPlayers + " actors, average response time: " + report.average / 1000000.0 + " ms.")
  }

  private def generatePlotReport(info: StressTestInfo) = {
    (new java.io.File(new java.io.File(info.outputFile).getParent)).mkdirs()
    val xs = reports.map(_.numPlayers).toSeq
    val ys = reports.map(_.average / 1000000.0).toSeq
    val plot = Plot.scatterPlot(xs, ys, "Average Response Times by Number of Actors", "Number of Actors", "Average Response Time (ns)")
    SVGRenderer(plot, info.outputFile, 600, 500)
  }
}