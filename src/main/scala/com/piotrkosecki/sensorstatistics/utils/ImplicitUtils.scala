package com.piotrkosecki.sensorstatistics.utils

object ImplicitUtils {
  // this will allow to put "Nones" always last
  implicit val optionsOrdering: Ordering[Option[Int]] = (x: Option[Int], y: Option[Int]) =>
    (x, y) match {
      case (Some(xx), Some(yy)) if xx == yy => 0
      case (Some(xx), Some(yy)) => if (xx > yy) -1 else 1
      case (Some(_), None) => -1
      case (None, Some(_)) => 1
      case _ => 0
    }
}
