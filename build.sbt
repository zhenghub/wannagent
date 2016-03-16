name := "wannagent"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.10.5"

val sprayVersion = "1.3.3"

val k = settingKey[File]("test file")

val akkaVersion = "2.3.14"

val dependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
  "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  "org.log4s" %% "log4s" % "1.2.1",
  "io.spray" %% "spray-http" % sprayVersion,
  "io.spray" %% "spray-can" % sprayVersion,
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

libraryDependencies ++= dependencies

k := baseDirectory.value / "conf"

resourceDirectories in Compile += k.value
