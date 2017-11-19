package clustering

import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._

object ModelPlayer {
  case object SendMessage
  
  def apply(timeKeeper:ActorRef):ModelPlayer = {
    new ModelPlayer(timeKeeper)
  }
}

class ModelPlayer private(private val timeKeeper:ActorRef) extends Actor {
  
  implicit val ec = context.system.dispatcher
  context.system.scheduler.schedule(0 seconds,100 millis,self,ModelPlayer.SendMessage)
  
  def receive = {
    case ModelPlayer.SendMessage => timeKeeper ! ModelTimeKeeper.ReceiveMessage
    case _ =>
  }
  
}