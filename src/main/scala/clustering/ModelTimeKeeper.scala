package clustering

import akka.actor.Actor
import akka.actor.ActorRef
import java.net.InetAddress
import scala.concurrent.duration._

object ModelTimeKeeper {
  case object ReceiveMessage
  case object PrintMessages
  
  def apply():ModelTimeKeeper = {
    new ModelTimeKeeper()
  }
}

class ModelTimeKeeper private() extends Actor {
  
  implicit val ec = context.system.dispatcher
  context.system.scheduler.schedule(0 seconds,1 second,self,ModelTimeKeeper.PrintMessages)
  
  var messages = 0
  
  def receive = {
    case ModelTimeKeeper.ReceiveMessage => messages += 1
    case ModelTimeKeeper.PrintMessages => {
      println(InetAddress.getLocalHost().toString.split("/")(1) + ", players: " + messages)
      messages = 0
    }
    case _ =>
  }
  
}