name := "sensor-statistics"

version := "0.1"

scalaVersion := "2.12.6"

val alpakkaVersion = "0.20"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.16",
  "com.lightbend.akka" %% "akka-stream-alpakka-file" % alpakkaVersion,
  "com.lightbend.akka" %% "akka-stream-alpakka-csv" % alpakkaVersion
)
