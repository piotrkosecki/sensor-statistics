package com.piotrkosecki.sensorstatistics

import java.nio.file.Paths
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Source}
import com.piotrkosecki.sensorstatistics.model.{Measurement, SensorStats}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Main {

  import utils.ImplicitUtils._

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("sensor-statistics-system")
    implicit val ec: ExecutionContext = system.dispatcher

    val path = getPath(args)
    val filePaths = getFilePaths(path)
    val filesCountFuture = countFiles(filePaths)
    val rawMeasurementsFuture = getRawMeasurements(filePaths)

    for {
      filesCount <- filesCountFuture
      (sensorStats, failedMeasurements) <- rawMeasurementsFuture
    } yield {
      printResults(filesCount, sensorStats, failedMeasurements)
      system.terminate()
    }
  }

  def getPath(args: Array[String]): String =
    Try(args(0)).getOrElse("src/main/resources/")

  def getFilePaths(path: String): Source[java.nio.file.Path, _] =
    Directory.ls(Paths.get(path)).filter(_.toString.endsWith(".csv"))

  def countFiles(filePaths: Source[java.nio.file.Path, _])(implicit mat: Materializer): Future[Int] =
    filePaths.runFold(0)((count, _) => count + 1)

  def getRawMeasurements(
      filePaths: Source[java.nio.file.Path, _]
  )(implicit mat: Materializer): Future[(Map[String, Option[SensorStats]], Int)] =
    filePaths.flatMapConcat { filename =>
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
          val updatedMap = measurementsMap.get(sId).flatten match {
            case Some(stats) => updateStats(measurementsMap, sId, hum, stats)
            case None => measurementsMap + (sId -> Some(SensorStats(hum, hum, hum, 1)))
          }
          updatedMap -> failed
        case ((measurementsMap, failed), Measurement(sId, None)) =>
          val updatedMap = measurementsMap.get(sId).flatten match {
            case Some(_) => measurementsMap
            case None => measurementsMap + (sId -> None)
          }
          updatedMap -> (failed + 1)
      }

  def updateStats(
      measurementsMap: Map[String, Option[SensorStats]],
      sId: String,
      hum: Int,
      stats: SensorStats
  ): Map[String, Option[SensorStats]] =
    measurementsMap.updated(
      sId,
      Some(SensorStats(math.min(stats.min, hum), math.max(stats.max, hum), stats.sum + hum, stats.count + 1))
    )

  def printResults(filesCount: Int, sensorStats: Map[String, Option[SensorStats]], failedMeasurements: Int): Unit = {
    val processedMeasurements = sensorStats.values.flatten.map(_.count).sum + failedMeasurements

    println(s"Num of processed files: $filesCount")
    println(s"Num of processed measurements: $processedMeasurements")
    println(s"Num of failed measurements: $failedMeasurements")
    println("\nSensors with highest avg humidity:\n")
    println("sensor-id,min,avg,max")

    sensorStats.toList.sortBy(_._2.map(stats => stats.sum / stats.count)).foreach {
      case (sId, Some(stats)) => println(s"$sId,${stats.min},${stats.sum / stats.count},${stats.max}")
      case (sId, None) => println(s"$sId,NaN,NaN,NaN")
    }
  }
}
