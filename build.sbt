name := "sensor-statistics"

version := "0.1"

scalaVersion := "2.13.2"

val alpakkaVersion = "2.0.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-stream"              % "2.6.5",
  "com.lightbend.akka"  %% "akka-stream-alpakka-file" % alpakkaVersion,
  "com.lightbend.akka"  %% "akka-stream-alpakka-csv"  % alpakkaVersion,
  "org.scalacheck"      %% "scalacheck"               % "1.14.1"        % "test"
)
