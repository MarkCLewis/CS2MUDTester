package tester

import scala.util.matching.Regex
import scala.xml.XML

case class IOConfig(
    numCommandsToGive: Int,
    commands: Seq[Command],
    roomOutput: Regex,
    inventoryOutput: Regex,
    roomName: IOElement,
    occupants: Option[IOElement],
    exits: IOElement,
    items: IOElement,
    invItems: IOElement) {
  
  private def validArgs(com: Command, state: MUDTestPlayer.GameState): Boolean = {
    com.args.forall(_.isValidForState(state))
  }

  def randomValidCommand(state: MUDTestPlayer.GameState): Command = {
    val com = commands(util.Random.nextInt(commands.length))
    if (com.isTerminator || (com.isMovement && !state.exits.contains(com.name)) || !validArgs(com, state))
      randomValidCommand(state) else com
  }

  def exitCommand(): Command = {
    commands.find(_.isTerminator).get
  }
}

case class IOElement(groupNumber: Int, separator: Option[String], pattern: Regex) {
  def parseSingle(m: Regex.Match): String = {
    m.group(groupNumber)
  }
  def parseSeq(m: Regex.Match): Seq[String] = {
    if (separator.isEmpty) throw new UnsupportedOperationException("No separator for element with parseSeq.")
    val str = m.group(groupNumber)
    println(s"str = $str")
    if (str == null) Seq.empty else {
      str.split(separator.get).map { part =>
        println(s"Match $part against $pattern")
        pattern.findFirstMatchIn(part) match {
          case None => throw new IllegalArgumentException("Part in parseSeq didn't match pattern.")
          case Some(m) => m.group(1)
        }
      }
    }
  }
}

object IOConfig {
  def apply(configFile: String): IOConfig = {
    val xml = XML.loadFile(configFile)
    val numCommandsToGive = (xml \ "numCommandsToGive").text.toInt
    val commands = (xml \ "commands" \ "command").flatMap { n =>
      val enabled = (n \ "@enabled").text
      if (enabled == "true") {
        val name = (n \ "@name").text
        val output = (n \ "@output").text
        val movement = (n \ "@movement").text == "true"
        output match {
          case "room" => Seq(RoomParsing(name, (n \ "argument").map(CommandArgument.apply), movement))
          case "inventory" => Seq(InvParsing(name, (n \ "argument").map(CommandArgument.apply)))
          case "unparsed" => Seq(Unparsed(name, (n \ "argument").map(CommandArgument.apply), false))
          case "terminate" => Seq(Unparsed(name, (n \ "argument").map(CommandArgument.apply), true))
          case _ => Nil
        }
      } else {
        Nil
      }
    }
    println(commands)
    val roomOutput = (xml \ "output" \ "roomOutput").text.trim
    val inventoryOutput = (xml \ "output" \ "inventoryOutput").text.trim
    val roomName = parseElement(xml \ "output" \ "roomName")
    val occupants = (xml \ "output" \ "occupants").headOption.map(parseElement)
    val exits = parseElement(xml \ "output" \ "exits")
    val items = parseElement(xml \ "output" \ "items")
    val invItems = parseElement(xml \ "output" \ "invItems")
    new IOConfig(numCommandsToGive, commands, roomOutput.r, inventoryOutput.r, roomName, occupants, exits, items, invItems)
  }

  def parseElement(n: xml.NodeSeq): IOElement = {
    val group = (n \ "@group").text.trim.toInt
    val separator = (n \ "@separator").headOption.map(_.text)
    val text = n.text
    val pattern = if (text.isEmpty) "(.*)" else text
    IOElement(group, separator, pattern.r)
  }
}