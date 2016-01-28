name := "wannagent"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.11.7"

val sprayVersion = "1.3.3"

val dependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  "org.log4s" %% "log4s" % "1.2.1",
  "io.spray" %% "spray-http" % sprayVersion,
  "io.spray" %% "spray-can" % sprayVersion,
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

libraryDependencies ++= dependencies
