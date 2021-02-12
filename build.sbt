import Dependencies._
import org.irundaia.sbt.sass._

name := "medical"

version := "1.0"

maintainer := "smart-medical@scala.uz"

SassKeys.cssStyle := Maxified

SassKeys.generateSourceMaps := false

SassKeys.syntaxDetection := ForceScss

includeFilter in(Assets, LessKeys.less) := "*.less"
excludeFilter in(Assets, LessKeys.less) := "_*.less"
lazy val scala212v = "2.12.8"
lazy val scala213v = "2.13.4"
lazy val supportedScalaVersions = List(scala212v, scala213v)
fork in run := true

lazy val `medical` = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    crossScalaVersions := supportedScalaVersions,
    scalaVersion := scala213v
  )

scalacOptions ++= CompilerOptions.cOptions

Global / onChangedBuildSource := ReloadOnSourceChanges

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.2" cross CrossVersion.full)

resolvers ++= Seq(
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/",
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= rootDependencies ++ Seq(jdbc, ehcache, ws, specs2 % Test, guice)