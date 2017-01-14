import scalariform.formatter.preferences._

name := "akka-streams-simple-chat"

version := "1.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.4.16",
  "io.gatling" % "gatling-app" % "2.2.3",
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.3" exclude("io.gatling", "gatling-recorder")
)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)

fork in run := true
