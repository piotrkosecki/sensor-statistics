package com.piotrkosecki.sensorstatistics

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.FileIO
import com.piotrkosecki.sensorstatistics.model.{Measurement, MeasurementStats, SensorStats}

import scala.concurrent.ExecutionContext
import scala.util.Try

object Main {

  import utils.ImplicitUtils._

  def main(args: Array[String]): Unit = {

    val path = Try(args(0)).toOption.getOrElse("src/main/resources/")
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = ExecutionContext.global

    val rawMeasurements = Directory.ls(Paths.get(path))
      .filter { filePath =>
        filePath.toString.endsWith(".csv")
      }
      .flatMapConcat { filename =>
        FileIO.fromPath(filename)
          .via(CsvParsing.lineScanner())
          .via(CsvToMap.toMapAsStrings())
      }
      .map(_.toList)
      .collect {
        case (_, sensorId) :: (_, humidity) :: Nil => Measurement(sensorId, Try(humidity.toInt).toOption)
      }
      .runFold(Map.empty[String, List[Option[Int]]]) {
        case (measurementsMap, Measurement(sId, hum)) =>
          measurementsMap.get(sId) match {
            case Some(value) => measurementsMap.updated(sId, hum :: value)
            case None => measurementsMap + (sId -> List(hum))
          }
      }

    // I could count with atomic Int but I didn't want to introduce variable,
    // other way would be to pass the file names through stream and then count them
    val filesCount = Directory.ls(Paths.get(path))
      .filter { filePath =>
        filePath.toString.endsWith(".csv")
      }.runFold(0) {
      case (count, _) => count + 1
    }

    val stats = rawMeasurements.map { measurementsMap =>
      val (succeeded, failed) = measurementsMap.flatMap(_._2).partition(_.isDefined)
      MeasurementStats(succeeded.size, failed.size) -> measurementsMap
        .toList
        .map {
          case (sid, measurements) =>
            sid -> (for {
              avg <- measurements.avg
              min = measurements.collect {
                case Some(value) => value
              }.min
              max = measurements.collect {
                case Some(value) => value
              }.max
            } yield SensorStats(min, avg, max))
        }.sortBy(_._2.map(-_.avg))
    }

    for {
      fCount <- filesCount
      (measurementStats, sensorStats) <- stats
    } {
      val processedMeasurements = measurementStats.failed + measurementStats.succeeded
      val failedMeasurements = measurementStats.failed

      println(s"Num of processed files: $fCount")
      println(s"Num of processed measurements: $processedMeasurements")
      println(s"Num of failed measurements: $failedMeasurements")
      println()
      println("Sensors with highest avg humidity:")
      println()
      println("sensor-id,min,avg,max")
      sensorStats.foreach {
        case (sId, stat) =>
          println(s"$sId,${stat.getOrElse("NaN,NaN,NaN")}")
      }
    }
  }

}
