package tester

object CommandArgument {
  def apply(n: xml.Node): CommandArgument = (n \ "@type").text match {
    case "roomItem" => RoomItem
    case "invItem" => InvItem
    case "occupant" => RoomOccupant
    case "exit" => RoomExit
    case "constant" => ConstantArg(n.text.trim)
  }
}

sealed trait CommandArgument {
  def apply(state: SimplePlayer.GameState): String
  def isValidForState(state: SimplePlayer.GameState): Boolean
}

case object InvItem extends CommandArgument {
  def apply(state: SimplePlayer.GameState): String = state.inventory(util.Random.nextInt(state.inventory.length))
  def isValidForState(state: SimplePlayer.GameState): Boolean = state.inventory.nonEmpty
}

case object RoomItem extends CommandArgument {
  def apply(state: SimplePlayer.GameState): String = state.roomItems(util.Random.nextInt(state.roomItems.length))
  def isValidForState(state: SimplePlayer.GameState): Boolean = state.roomItems.nonEmpty
}

case object RoomOccupant extends CommandArgument {
  def apply(state: SimplePlayer.GameState): String = state.players(util.Random.nextInt(state.players.length))
  def isValidForState(state: SimplePlayer.GameState): Boolean = state.players.nonEmpty
}

case object RoomExit extends CommandArgument {
  def apply(state: SimplePlayer.GameState): String = state.exits(util.Random.nextInt(state.exits.length))
  def isValidForState(state: SimplePlayer.GameState): Boolean = state.exits.nonEmpty
}

case class ConstantArg(value: String) extends CommandArgument {
  def apply(state: SimplePlayer.GameState): String = value 
  def isValidForState(state: SimplePlayer.GameState): Boolean = true
}