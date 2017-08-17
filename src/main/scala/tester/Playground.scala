package tester

object Playground extends App {
  val output = """Enter a username.
Welcome MUDTESTPLAYER.
Living Room
The living room is where people live.
Exits: south
Items: flyer
MUDTESTPLAYER has entered the Living Room.

"""

  val regex = """([^\n]+)
(.+)(
Players: (.+))?
Exits: (.+)(
Items: (.+))?
""".r.unanchored

println(regex)
  val m = regex.findFirstMatchIn(output)
  println(m.map(_.group(1)))
  println(m.map(_.group(2)))
  println(m.map(_.group(4)))
  println(m.map(_.group(5)))
  println(m.map(_.group(7)))
  
  
}