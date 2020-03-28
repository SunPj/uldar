import Dependencies._

ThisBuild / name               := "uldar"
ThisBuild / description        := "A tiny library to extend the application with CMS functionality"
ThisBuild / version            := "0.1-SNAPSHOT"
ThisBuild / organization       := "com.github.sunpj"
ThisBuild / homepage           := Some(url("https://github.com/SunPj/uldar"))
ThisBuild / scmInfo            := Some(ScmInfo(url("https://github.com/SunPj/uldar"), "https://github.com/SunPj/uldar.git"))
ThisBuild / developers         := List(Developer("auldanov", "Aidar Uldanov", "root@sunsongs.ru", url("https://github.com/SunPj")))
ThisBuild / licenses           += "GPLv2" -> url("https://www.gnu.org/licenses/gpl-2.0.html")
ThisBuild / scalaVersion       := "2.13.1"
ThisBuild / crossScalaVersions := Seq(scalaVersion.value, "2.11.12")

// To sync with Maven central, you need to supply the following information:
publishMavenStyle := true

publishTo := sonatypePublishToBundle.value

import xerial.sbt.Sonatype._
sonatypeProfileName := "com.github.sunpj"
sonatypeProjectHosting := Some(GitHubHosting("SunPj", "uldar", "root@sunsongs.ru"))

lazy val root = (project in file("."))
  .settings(
    name := "uldar",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "javax.inject" % "javax.inject" % "1",
      "org.slf4j" % "slf4j-api" % "1.7.9",
      "com.typesafe.play" %% "play-json" % "2.8.1"
    ) 
  )

