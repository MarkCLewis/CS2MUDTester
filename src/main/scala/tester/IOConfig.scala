package tester

import scala.util.matching.Regex
import scala.xml.XML

case class IOConfig(
    commands: Seq[Command], 
    promptFormat: String, 
    roomOutput: Regex, 
    inventoryOutput: Regex,
    roomName: IOElement,
    occupants: Option[IOElement],
    exits: IOElement,
    items: IOElement)

case class IOElement(groupNumber: Int, multiline: Boolean, separator:Option[String], pattern: Regex)

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
    val prompt = (xml \ "output" \ "prompt").text.trim
    val roomOutput = (xml \ "output" \ "roomOutput").text.trim
    val inventoryOutput = (xml \ "output" \ "inventoryOutput").text.trim
    val roomName = parseElement(xml \ "output" \ "roomName")
    val occupants = (xml \ "output" \ "occupants").headOption.map(parseElement)
    val exits = parseElement(xml \ "output" \ "exits")
    val items = parseElement(xml \ "output" \ "items")
    new IOConfig(commands, prompt, roomOutput.r, inventoryOutput.r, roomName, occupants, exits, items)
  }
  
  def parseElement(n: xml.NodeSeq): IOElement = {
    val group = (n \ "@group").text.trim.toInt
    val multiline = (n \ "@multiline").headOption.map(_.text.toBoolean).getOrElse(false)
    val separator = (n \ "@separator").headOption.map(_.text)
    val text = n.text
    val pattern = if(text.isEmpty) ".*" else text
    IOElement(group, multiline, separator, pattern.r)
  }
}