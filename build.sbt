name := "CSCI1321 MUD Tester"

version := "1.0"

scalaVersion := "2.12.3"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.4"
libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.12" % "1.0.6" excludeAll ExclusionRule(name = "embrace")
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.6"
libraryDependencies += "com.typesafe.akka" % "akka-cluster-metrics_2.12" % "2.5.6"
libraryDependencies += "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.5.3"

addCommandAlias("pandora00", "runMain stresser.MUDStress pandora00")
addCommandAlias("pandora01", "runMain stresser.MUDStress pandora01")
addCommandAlias("pandora02", "runMain stresser.MUDStress pandora02")
addCommandAlias("pandora03", "runMain stresser.MUDStress pandora03")
addCommandAlias("pandora04", "runMain stresser.MUDStress pandora04")
addCommandAlias("pandora05", "runMain stresser.MUDStress pandora05")
addCommandAlias("pandora06", "runMain stresser.MUDStress pandora06")
addCommandAlias("pandora07", "runMain stresser.MUDStress pandora07")
addCommandAlias("pandora08", "runMain stresser.MUDStress pandora08")