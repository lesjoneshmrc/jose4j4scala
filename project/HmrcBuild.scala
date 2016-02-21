import sbt.Keys._
import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning


object HmrcBuild extends Build {

  val appName = "jose4j4scala"

  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  lazy val scoverageSettings = {
    import scoverage.ScoverageSbtPlugin._

    // Semicolon-separated list of regexs matching classes to exclude
    Seq(
      ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*(BuildInfo|Routes).*",
      ScoverageKeys.coverageMinimum := 80,
      ScoverageKeys.coverageFailOnMinimum := false,
      ScoverageKeys.coverageHighlighting := true
    )
  }

  lazy val Jose4J4Scala = Project(appName, file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(playSettings ++ scoverageSettings : _*)
    .settings(
      scalaVersion := "2.11.7",
      libraryDependencies ++= LibDependencies(),
      crossScalaVersions := Seq("2.11.7"),
        resolvers := Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/"
        )
  )
}

private object LibDependencies {

  import play.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    "com.typesafe.play" %% "play" % PlayVersion.current,
    ws,
    "org.bitbucket.b_c" % "jose4j" % "0.4.4"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % "2.2.5" % scope,
        "org.scalatestplus" %% "play" % "1.2.0" % scope,
        "org.mockito" % "mockito-all" % "1.9.5" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalacheck" %% "scalacheck" % "1.12.2" % scope,
        "org.pegdown" % "pegdown" % "1.5.0" % scope,
        "com.github.tomakehurst" % "wiremock" % "1.52" % scope,
        "uk.gov.hmrc" %% "hmrctest" % "1.4.0"
      )
    }.test
  }

  def apply() = compile ++ Test()
}

