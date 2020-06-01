package com.piotrkosecki.sensorstatistics

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.FileIO
import com.piotrkosecki.sensorstatistics.model.{Measurement, SensorStats}

import scala.concurrent.ExecutionContext
import scala.util.Try

object Main {

  import utils.ImplicitUtils._

  def main(args: Array[String]): Unit = {

    val path = Try(args(0)).toOption.getOrElse("src/main/resources/")
    implicit val system: ActorSystem = ActorSystem()
    implicit val ec: ExecutionContext = ExecutionContext.global

    val filePaths = Directory.ls(Paths.get(path)).filter { filePath =>
      filePath.toString.endsWith(".csv")
    }

    val filesCount = filePaths.runFold(0) {
      case (count, _) => count + 1
    }

    val rawMeasurements = filePaths.flatMapConcat { filename =>
      FileIO
        .fromPath(filename)
        .via(CsvParsing.lineScanner())
        .via(CsvToMap.toMapAsStrings())
    }.map(_.toList)
      .collect {
        case (_, sensorId) :: (_, humidity) :: Nil => Measurement(sensorId, Try(humidity.toInt).toOption)
      }
      .runFold((Map.empty[String, Option[SensorStats]], 0)) {
        case ((measurementsMap, failed), Measurement(sId, Some(hum))) =>
          measurementsMap.get(sId).flatten match {
            case Some(SensorStats(min, max, sum, count)) =>
              measurementsMap.updated(
                sId,
                Some(SensorStats(math.min(min, hum), math.max(max, hum), sum + hum, count + 1))
              ) -> failed
            case None => (measurementsMap + (sId -> Some(SensorStats(hum, hum, hum, 1)))) -> failed
          }
        case ((measurementsMap, failed), Measurement(sId, None)) =>
          measurementsMap.get(sId).flatten match {
            case Some(_) => measurementsMap -> (failed + 1)
            case None => (measurementsMap + (sId -> None)) -> 1
          }
      }

    for {
      fCount <- filesCount
      (sensorStats, failedMeasurements) <- rawMeasurements
    } {
      val processedMeasurements = sensorStats.values.flatMap(_.toList).map(_.count).sum + failedMeasurements

      println(s"Num of processed files: $fCount")
      println(s"Num of processed measurements: $processedMeasurements")
      println(s"Num of failed measurements: $failedMeasurements")
      println()
      println("Sensors with highest avg humidity:")
      println()
      println("sensor-id,min,avg,max")
      sensorStats.toList.sortBy(_._2.map(stats => stats.sum / stats.count)).foreach {
        case (sId, stat) =>
          println(s"$sId,${stat.getOrElse("NaN,NaN,NaN")}")
      }
      system.terminate()
    }
  }

}
