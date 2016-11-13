import sbt._

object Settings {
  val name = "wannagent-web"
  val version = "0.0.1"

  object versions {
    val scala = "2.11.8"
    val playScripts = "0.4.0"
  }

  /** Options for the scala compiler */
  val scalacOptions = Seq(
    "-Xlint",
    "-unchecked",
    "-deprecation",
    "-feature"
  )

  /** Dependencies only used by the JVM project */
  val jvmDependencies = Def.setting(Seq(
    "com.vmunier" %% "play-scalajs-scripts" % versions.playScripts,
    "jp.t2v" %% "play2-auth"        % "0.14.2",
    play.sbt.Play.autoImport.cache
  ))
}