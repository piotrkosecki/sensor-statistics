package com.piotrkosecki.sensorstatistics

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.Materializer
import com.piotrkosecki.sensorstatistics.model.SensorStats
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.Paths

class MainSpec extends AsyncFlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  implicit val system: ActorSystem = ActorSystem("test-system")
  implicit val mat: Materializer = Materializer(system)

  override def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }

  "getPath" should "return the given path if provided" in {
      val args = Array("some/path")
      val path = Main.getPath(args)
      path shouldEqual "some/path"
    }

  it should "return default path if no argument is provided" in {
      val args = Array.empty[String]
      val path = Main.getPath(args)
      path shouldEqual "src/main/resources/"
    }

  "getFilePaths" should "return a Source of file paths ending with .csv" in {
      val path = "src/test/resources/"
      val filePaths = Main.getFilePaths(path)
      val result = filePaths.runWith(Sink.seq).futureValue
      result should contain(Paths.get("src/test/resources/test.csv"))
    }

  "countFiles" should "correctly count the number of files" in {
      val filePaths = Source(List(Paths.get("file1.csv"), Paths.get("file2.csv")))
      val fileCountFuture = Main.countFiles(filePaths)
      fileCountFuture.map { count =>
        count shouldEqual 2
      }
    }

  "updateStats" should "correctly update sensor statistics" in {
      val measurementsMap = Map[String, Option[SensorStats]]()
      val sId = "sensor1"
      val hum = 30
      val stats = SensorStats(25, 35, 90, 3)
      val updatedMap = Main.updateStats(measurementsMap, sId, hum, stats)
      updatedMap(sId) shouldEqual Some(SensorStats(25, 35, 120, 4))
    }

  "printResults" should "print the correct results" in {
      val filesCount = 1
      val sensorStats = Map(
        "sensor1" -> Some(SensorStats(10, 50, 100, 3)),
        "sensor2" -> None
      )
      val failedMeasurements = 1

      // Capture the console output
      val stream = new java.io.ByteArrayOutputStream()
      Console.withOut(stream) {
        Main.printResults(filesCount, sensorStats, failedMeasurements)
      }
      val output = stream.toString

      output should include("Num of processed files: 1")
      output should include("Num of processed measurements: 4")
      output should include("Num of failed measurements: 1")
      output should include("sensor1,10,33,50")
      output should include("sensor2,NaN,NaN,NaN")
    }

  "getRawMeasurements" should "correctly process measurements" in {
      val filePaths = Source.single(Paths.get("src/test/resources/test.csv"))
      val result = Main.getRawMeasurements(filePaths).futureValue

      val expectedStats = Map(
        "s1" -> Some(SensorStats(10, 10, 10, 1)),
        "s2" -> None,
        "s3" -> Some(SensorStats(85, 85, 85, 1)),
        "s4" -> None
      )
      val expectedFailed = 2

      result._1 shouldEqual expectedStats
      result._2 shouldEqual expectedFailed
    }
}
