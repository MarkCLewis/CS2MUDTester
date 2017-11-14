package clustering

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import java.net.InetAddress
import java.io.PrintStream
import java.io.BufferedReader
import java.net.Socket
import java.io.InputStreamReader

object SimpleClusterApp {
  def main(args: Array[String]): Unit = {
    startup(args)

  }

  def startup(ports: Seq[String]): Unit = {
    ports foreach { port =>
      val config = ConfigFactory.parseString("akka.remote.netty.hostname=" + InetAddress.getLocalHost.getHostName).withFallback(ConfigFactory.load())
      val system = ActorSystem("ClusterSystem", config)
      system.actorOf(Props[SimpleClusterListener], name = "clusterListener")

      val sock = new Socket("131.194.71.97", 4000)
      val in = new BufferedReader(new InputStreamReader(sock.getInputStream()))
      val out = new PrintStream(sock.getOutputStream())
      
      if(in.ready()) println(in.readLine())
      out.println(InetAddress.getLocalHost.getHostName)
      if(in.ready()) println(in.readLine())
    }
  }

}
