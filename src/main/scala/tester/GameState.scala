package tester

object GameState {
  
  def apply() = new GameState(Nil,Nil,Nil,Nil)
}

class GameState private(val players:List[MUDTestPlayer],
    val inventory:List[String],
    val items:List[String],
    val exits:List[String]) {
  
}