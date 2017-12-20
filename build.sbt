name := "CSCI1321MUDTester"

version := "1.0"

scalaVersion := "2.12.3"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.8"
libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.12" % "1.0.6" excludeAll ExclusionRule(name = "embrace")
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.8"
libraryDependencies += "com.typesafe.akka" % "akka-cluster-metrics_2.12" % "2.5.8"
libraryDependencies += "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.5.8"
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.5.8"

addCommandAlias("pandora00", "runMain stresserclustering.MUDStress pandora00")
addCommandAlias("pandora01", "runMain stresserclustering.MUDStress pandora01")
addCommandAlias("pandora02", "runMain stresserclustering.MUDStress pandora02")
addCommandAlias("pandora03", "runMain stresserclustering.MUDStress pandora03")
addCommandAlias("pandora04", "runMain stresserclustering.MUDStress pandora04")
addCommandAlias("pandora05", "runMain stresserclustering.MUDStress pandora05")
addCommandAlias("pandora06", "runMain stresserclustering.MUDStress pandora06")
addCommandAlias("pandora07", "runMain stresserclustering.MUDStress pandora07")
addCommandAlias("pandora08", "runMain stresserclustering.MUDStress pandora08")