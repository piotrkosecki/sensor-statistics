package com.piotrkosecki.sensorstatistics.model

case class SensorStats(min: Int, max: Int, sum: Int, count: Int) {
  override def toString: String = s"$min,${sum / count},$max"
}
