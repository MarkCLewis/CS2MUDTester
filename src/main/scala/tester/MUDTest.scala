package tester

import java.net.Socket

/**
 * This application will test a networked MUD implementation. The user needs to provide connection
 * information, host and port, as well as information about what commands are to be tested.
 */
object MUDTest extends App {
  val flagsAndValues = args.zip(args.tail).foldLeft(Map[String, String]()) { (m, t) =>
    if(t._1(0) == '-') m + (t._1 -> t._2) else m
  }
  val requiredFlags = "-host -port".split(" ")
  val allRequired = for(flag <- requiredFlags) yield {
    if(!flagsAndValues.contains(flag)) {
      println(flag + " is a required setting.")
      false
    } else true
  }
  if(allRequired.exists(!_)) sys.exit(1)
  
  // Figure out which commands we are testing.

  // TODO - this will make a connection
  val sock = new Socket(flagsAndValues("-host"), flagsAndValues("-port").toInt)
  
}

// TODO - need an abstraction for commands.
// TODO - need an abstraction for connections.