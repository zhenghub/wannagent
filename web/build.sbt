import sbt.Keys._
import sbt.Project.projectToRef

scalaVersion := "2.11.8"

//lazy val root = (project in file(".")).enablePlugins(PlayScala)

// instantiate the JS project for SBT with some additional settings
lazy val client: Project = (project in file("client"))
  .settings(
    name := "client",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    scalacOptions ++= Settings.scalacOptions,
    libraryDependencies ++= Seq(
      "be.doeraene" %%% "scalajs-jquery" % "0.9.0",
      "com.lihaoyi" %%% "scalatags" % "0.6.0",
      "com.lihaoyi" %%% "scalarx" % "0.3.1",
      "com.github.karasiq" %%% "scalajs-bootstrap" % "1.1.2"
    )
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
//.dependsOn(sharedJS)

// Client projects (just one in this case)
lazy val clients = Seq(client)

// instantiate the JVM project for SBT with some additional settings
lazy val server = (project in file("server"))
  .settings(
    name := "server",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    scalacOptions ++= Settings.scalacOptions,
    libraryDependencies ++= Settings.jvmDependencies.value,
    //commands += ReleaseCmd,
    // connect to the client project
    scalaJSProjects := clients
      pipelineStages in Assets := Seq (scalaJSPipeline)
    // compress CSS
    //LessKeys.compress in Assets := true
  )
  .enablePlugins(PlayScala, SbtWeb)
  .aggregate(clients.map(projectToRef): _*)
//.dependsOn(sharedJVM)