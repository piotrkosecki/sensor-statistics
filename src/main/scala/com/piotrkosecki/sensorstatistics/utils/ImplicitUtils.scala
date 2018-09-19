package com.piotrkosecki.sensorstatistics.utils

import scala.util.Try

object ImplicitUtils {
  implicit class ListOfOptionsHelper(list: List[Option[Int]]) {
    def avg: Option[Int] = {
      val sum = list.collect {
        case Some(intValue) => intValue
      }.sum
      val count = list.count(_.isDefined)
      Try(sum/count).toOption
    }
  }

  implicit val optionsOrdering: Ordering[Option[Int]] = (x: Option[Int], y: Option[Int]) => (x, y) match {
    case (Some(xx), Some(yy)) => if (xx > yy) 1 else -1
    case (Some(_), None) => -1
    case (None, Some(_)) => 1
    case _ => 0
  }
}
