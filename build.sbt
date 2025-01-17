
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "play-template"
  )
  .enablePlugins(PlayScala)

resolvers += "HMRC-open-artefacts-maven2" at "https://open.artefacts.tax.service.gov.uk/maven2"

libraryDependencies ++= Seq(
  "uk.gov.hmrc.mongo"      %% "hmrc-mongo-play-28"   % "0.63.0",
  guice,
  "org.scalatest"          %% "scalatest"               % "3.2.15"             % Test,
  "org.scalamock"          %% "scalamock"               % "5.2.0"             % Test,
  "org.scalatestplus.play" %% "scalatestplus-play"   % "5.1.0"          % Test,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.33.2" % Test
)

// for connectors
libraryDependencies += ws
libraryDependencies += ("org.typelevel" %% "cats-core" % "2.3.0")

libraryDependencies += "eu.timepit" %% "refined" % "0.11.3"
libraryDependencies += "be.venneborg" %% "play28-refined" % "0.6.0"

dependencyOverrides +="com.fasterxml.jackson.core" % "jackson-databind" % "2.11.0"
