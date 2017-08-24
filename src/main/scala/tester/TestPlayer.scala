package tester

import java.io.{PrintStream,BufferedReader}
import akka.actor.Actor

object TestPlayer {
  def apply(n:String,i:BufferedReader,o:PrintStream,c:IOConfig):TestPlayer = {
    new TestPlayer(n,i,o,c)
  }
}

class TestPlayer private(name:String,
    private val in:BufferedReader,
    private val out:PrintStream,
    val config:IOConfig) extends Player(name,in,out,config) {
  
    def connect() {
      
    }
    
    def takeAction() {
      
    }
  
}