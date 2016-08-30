name := "wannagent"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.11.8"

val k = settingKey[File]("test file")

val sprayVersion = "1.3.3"
val akkaVersion = "2.4.9"

lazy val dependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion withSources(),
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion withSources(),
  "com.typesafe.akka" %% "akka-http-core" % akkaVersion withSources(),
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion withSources(),

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
