package tester

import scala.util.matching.Regex
import scala.xml.XML

case class IOConfig(
    commands: Seq[Command], 
    roomOutput: Regex, 
    inventoryOutput: Regex,
    roomName: IOElement,
    occupants: Option[IOElement],
    exits: IOElement,
    items: IOElement,
    invItems: IOElement)

case class IOElement(groupNumber: Int, separator:Option[String], pattern: Regex) {
  def parseSingle(m: Regex.Match): String = {
    m.group(groupNumber)
  }
  def parseSeq(m: Regex.Match): Seq[String] = {
    if(separator.isEmpty) throw new UnsupportedOperationException("No separator for element with parseSeq.")
    val str = m.group(groupNumber)
    str.split(separator.get).map { part =>
      pattern.findFirstMatchIn(part) match {
        case None => throw new IllegalArgumentException("Part in parseSeq didn't match pattern.")
        case Some(m) => m.group(1)
      }
    }
  }
}

object IOConfig {
  def apply(configFile: String): IOConfig = {
    val xml = XML.loadFile(configFile)
    val commands = (xml \ "commands" \ "command").flatMap { n =>
      val enabled = (n \ "@enabled").text
      if(enabled=="true") {
        val name = (n \ "@name").text
        val output = (n \ "@type").text
        output match {
          case "room" => Seq(RoomParsing(name, (n \ "argument").map(CommandArgument.apply)))
          case "inventory" => Seq(InvParsing(name, (n \ "argument").map(CommandArgument.apply)))
          case "unparsed" => Seq(Unparsed(name, (n \ "argument").map(CommandArgument.apply)))
          case _ => Nil
        }
      } else {
        Nil
      }
    }
    val roomOutput = (xml \ "output" \ "roomOutput").text.trim
    val inventoryOutput = (xml \ "output" \ "inventoryOutput").text.trim
    val roomName = parseElement(xml \ "output" \ "roomName")
    val occupants = (xml \ "output" \ "occupants").headOption.map(parseElement)
    val exits = parseElement(xml \ "output" \ "exits")
    val items = parseElement(xml \ "output" \ "items")
    val invItems = parseElement(xml \ "output" \ "invItems")
    new IOConfig(commands, roomOutput.r, inventoryOutput.r, roomName, occupants, exits, items, invItems)
  }
  
  def parseElement(n: xml.NodeSeq): IOElement = {
    val group = (n \ "@group").text.trim.toInt
    val separator = (n \ "@separator").headOption.map(_.text)
    val text = n.text
    val pattern = if(text.isEmpty) ".*" else text
    IOElement(group, separator, pattern.r)
  }
}