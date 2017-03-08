name := "wannagent"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.11.8"

val k = settingKey[File]("test file")

val sprayVersion = "1.3.3"
val akkaVersion = "2.4.17"
val akkahttpVersion = "10.0.4"

lazy val dependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion withSources(),
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion withSources(),
  "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
  "org.scalatest" %% "scalatest" % "2.2.4" % Test,
  "org.log4s" %% "log4s" % "1.2.1",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.backuity.clist" %% "clist-core"   % "3.2.2",
  "org.backuity.clist" %% "clist-macros" % "3.2.2" % "provided"
)

libraryDependencies ++= dependencies

k := baseDirectory.value / "conf"

resourceDirectories in Compile += k.value

libraryDependencies += "com.lihaoyi" % "ammonite" % "0.8.2" % "test" cross CrossVersion.full

initialCommands in (Test, console) := """ammonite.Main().run()"""