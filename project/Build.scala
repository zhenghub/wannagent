import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {

  lazy val root = Project(
    id = "wannagent",
    base = file("."),
    settings = Defaults.defaultSettings
      ++ packAutoSettings // This settings add pack and pack-archive commands to sbt
      ++ Seq(
        packMain := Map("wannagent" -> "org.freefeeling.wannagent.ProxyServer"), 
        // [Optional] Extra class paths to look when launching a program. You can use ${PROG_HOME} to specify the base directory
        packExtraClasspath := Map("wannagent" -> Seq("${PROG_HOME}/conf")),
        // [Optional] Resource directory mapping to be copied within target/pack. Default is Map("{projectRoot}/src/pack" -> "")
        packResourceDir := Map(baseDirectory.value / "conf" -> "conf")
      )
  )
}
