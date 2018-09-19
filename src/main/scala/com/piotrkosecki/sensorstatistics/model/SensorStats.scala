package com.piotrkosecki.sensorstatistics.model

case class SensorStats(min: Int, avg: Int, max: Int) {
  override def toString: String = s"$min,$avg,$max"
}
