package stresser.remoting

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.ServerSocket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import utility.IOConfig
import scala.collection.mutable.Queue
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory

object MUDStress extends App {

  val system = ActorSystem("MUDStressTest")
  val config = ConfigFactory.load("local")
  val testManager = system.actorOf(Props(StressTestManager()), "StressTestManager")

  Future {
    checkConnections()
  }

  def checkConnections() {
    val ss = new ServerSocket(6666)
    while (true) {
      val sock = ss.accept()
      val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
      val out = new PrintStream(sock.getOutputStream())
      Future {
        out.println("Host?")
        val host = in.readLine()
        out.println("Port?")
        val port = in.readLine().toInt
        out.println("Path to config?")
        val ioConfig = in.readLine().trim match {
          case "" => IOConfig("config.xml")
          case path => IOConfig(path)
        }
        testManager ! StressTestManager.EnqueueStressTestInfo(new StressTestInfo(in, out, host, port, ioConfig))
      }
    }
  }
}