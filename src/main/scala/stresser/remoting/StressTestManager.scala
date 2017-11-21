package stresser.remoting

import akka.actor.Actor
import scala.collection.mutable.Queue
import java.io.PrintStream
import java.io.BufferedReader
import akka.actor.Props
import utility.IOConfig
import akka.actor.Deploy
import akka.remote.RemoteScope
import akka.actor.Address
import scala.concurrent.duration._
import utility.ResponseReport

case class StressTestInfo(in: BufferedReader, out: PrintStream, host: String, port: Int, ioConfig: IOConfig)

object StressTestManager {
  case class EnqueueStressTestInfo(info: StressTestInfo)
  case object RunNextStressTest
  case class ReceiveTestReport(report:ResponseReport)

  def apply(): StressTestManager = {
    new StressTestManager()
  }
}

class StressTestManager private () extends Actor {
  implicit private val ec = context.system.dispatcher
  context.system.scheduler.schedule(1 second, 10 seconds, self, StressTestManager.RunNextStressTest)

  private val tests = Queue[StressTestInfo]()
  private val nodes = "0 1 2 3 4 5 6 7 8".split(" ").map("131.194.71.13" + _.trim)
  private var testing = false

  def receive = {
    case StressTestManager.EnqueueStressTestInfo(info) => tests.enqueue(info)
    case StressTestManager.RunNextStressTest => {
      if(tests.size>0 && !testing) {
        testing = true
        stressTest(tests.dequeue())
      }
    }
    case StressTestManager.ReceiveTestReport(report) => {
      testing = false
    }
    case _ =>
  }

  private def stressTest(info: StressTestInfo) {
    info.out.println("Running networked stress test.")
    val timeKeeper = context.system.actorOf(Props(TimeKeeper(info.in,info.out,self)), "TimeKeeper")
    val playerManagers = nodes.map { node =>
      val address = Address("akka.tcp", "MUDStressTest", node, 5150)
      context.system.actorOf(Props(StressPlayerManager(info.ioConfig, info.host, info.port, timeKeeper, node)).withDeploy(Deploy(scope = RemoteScope(address))))
    }
  }

}