package utility

import akka.actor.ActorRef

object PlayerManager {
  case class RegisterPlayer(player: ActorRef)
  case class DeregisterPlayer(player: ActorRef)
}