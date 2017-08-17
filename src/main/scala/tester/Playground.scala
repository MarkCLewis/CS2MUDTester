package tester

object Playground extends App {
  val output = """Living Room
The living room is where people live.
Players: Mark
Exits: south
Items: flyer
>
"""

  val regex = """([^\n]+)
(.+?)
Players: (.+)
Exits: (.+)
(Items: (.+))?
""".r.unanchored

  println(regex)
  val m = regex.findFirstMatchIn(output)
  println(m.map(_.group(1)))
  println(m.map(_.group(2)))
  println(m.map(_.group(3)))
  println(m.map(_.group(4)))
  println(m.map(_.group(5)))
  println(m.map(_.group(6)))

  
}