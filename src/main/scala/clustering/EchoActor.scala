package clustering

import akka.actor.AbstractActor.Receive
import akka.actor.ActorLogging
import akka.actor.Actor

class EchoActor extends Actor with ActorLogging {
  def receive: Receive = {
    case message =>
      log.info("Received Message {} in Actor {}", message, self.path.name)
  }
}