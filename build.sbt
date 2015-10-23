name := "wannagent"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.11.7"

val k = settingKey[File]("test file")

//val sprayVersion = settingKey[String]("spray version")
//val akkaVersion = settingKey[String]("spray version")
//
//val adjustVersion = taskKey[Unit]("adjust dependencies according to scala version")
//
//adjustVersion := {
//  val v = scalaVersion.value.split('.')(1).toInt
//  if (v > 10) {
//    resolvers += Resolver.bintrayRepo("hseeberger", "maven")
//    libraryDependencies += ("de.heikoseeberger" %% "akka-macro-logging" % "0.2.1")
//    sprayVersion := "1.3.3"
//    akkaVersion := "2.3.14"
//  } else {
//    sprayVersion := "1.3.3"
//    akkaVersion := "2.3.14"
//  }
//}

val sprayVersion = "1.3.3"
val akkaVersion = "2.3.15"

lazy val dependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,

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
