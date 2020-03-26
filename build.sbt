import Dependencies._

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.github.sunpj"

lazy val root = (project in file("."))
  .settings(
    name := "uldar",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "javax.inject" % "javax.inject" % "1",
      "com.typesafe.play" %% "play-json" % "2.8.1"
    ) 
  )

