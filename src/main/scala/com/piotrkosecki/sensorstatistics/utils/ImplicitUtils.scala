package com.piotrkosecki.sensorstatistics.utils

object ImplicitUtils {
// this will allow to put "Nones" always last
  implicit val optionsOrdering: Ordering[Option[Int]] = (x: Option[Int], y: Option[Int]) =>
    (x, y) match {
      case (Some(xx), Some(yy)) => yy.compare(xx) // Descending order for Some values
      case (Some(_), None) => -1 // Some is less than None
      case (None, Some(_)) => 1 // None is greater than Some
      case (None, None) => 0 // Equal if both are None
    }
}
