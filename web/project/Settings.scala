import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

object Settings {
  val name = "wannagent-web"
  val version = "0.0.1"

  object versions {
    val scala = "2.11.8"
    val scalajsScripts = "1.0.0"
    val autowire = "0.2.5"
    val booPickle = "1.2.5"
  }

  /** Options for the scala compiler */
  val scalacOptions = Seq(
    "-Xlint",
    "-unchecked",
    "-deprecation",
    "-feature"
  )

  val sharedDependencies = Def.setting(Seq(
    "com.lihaoyi" %%% "autowire" % versions.autowire,
    "me.chrons" %%% "boopickle" % versions.booPickle
  ))

  /** Dependencies only used by the JVM project */
  val jvmDependencies = Def.setting(Seq(
    "com.vmunier" %% "scalajs-scripts" % versions.scalajsScripts,
    "jp.t2v" %% "play2-auth"        % "0.14.2",
    play.sbt.Play.autoImport.cache
  ))
}
