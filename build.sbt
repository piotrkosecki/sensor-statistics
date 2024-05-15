name := "sensor-statistics"

version := "0.1"

scalaVersion := "2.13.14"

val alpakkaVersion = "6.0.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-stream"              % "2.8.5",
  "com.lightbend.akka"  %% "akka-stream-alpakka-file" % alpakkaVersion,
  "com.lightbend.akka"  %% "akka-stream-alpakka-csv"  % alpakkaVersion,
  "org.scalacheck"      %% "scalacheck"               % "1.18.0"        % "test",
  "org.scalatest"       %% "scalatest"                % "3.2.18"        % "test"
)
