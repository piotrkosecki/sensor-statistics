name := "sensor-statistics"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.16"

libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.20"

libraryDependencies += "com.lightbend.akka" % "akka-stream-alpakka-csv_2.12" % "0.20"
