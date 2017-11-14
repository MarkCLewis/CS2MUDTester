name := "CSCI1321 MUD Tester"

version := "1.0"

scalaVersion := "2.12.3"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.4"
libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.12" % "1.0.6" excludeAll ExclusionRule(name = "embrace")
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.6"
libraryDependencies += "com.typesafe.akka" % "akka-cluster-metrics_2.12" % "2.5.6"