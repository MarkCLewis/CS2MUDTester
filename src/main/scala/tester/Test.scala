package tester

import utility.Command

trait Test {
  def start(): String = "Running " + this.toString() + " procedure."
  def success(): String = "Successful " + this.toString() + " procedure."
  def fail(command: Command) = "Unsuccessful " + this.toString() + " failed on \"" + command.name + "\" command."
}